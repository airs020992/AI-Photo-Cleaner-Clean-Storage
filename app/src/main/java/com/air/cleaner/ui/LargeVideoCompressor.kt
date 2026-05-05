package com.air.cleaner.ui

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.air.cleaner.domain.cleaning.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal interface LargeVideoCompressor {
    suspend fun compress(
        request: LargeVideoCompressionRequest,
        onProgress: (LargeVideoCompressionProgress) -> Unit,
    ): List<LargeVideoCompressionResult>
}

internal data class LargeVideoCompressionRequest(
    val videos: List<MediaItem>,
    val profile: LargeVideoCompressionProfile,
)

internal data class LargeVideoCompressionProgress(
    val completedCount: Int,
    val activeItemProgress: Float,
)

@OptIn(UnstableApi::class)
internal class Media3LargeVideoCompressor(
    private val context: Context,
) : LargeVideoCompressor {
    override suspend fun compress(
        request: LargeVideoCompressionRequest,
        onProgress: (LargeVideoCompressionProgress) -> Unit,
    ): List<LargeVideoCompressionResult> {
        return withContext(Dispatchers.Main.immediate) {
            val results = mutableListOf<LargeVideoCompressionResult>()
            request.videos.forEachIndexed { index, video ->
                coroutineContext.ensureActive()
                val result = compressOne(
                    video = video,
                    profile = request.profile,
                    completedCount = index,
                    onProgress = onProgress,
                )
                results += result
                onProgress(LargeVideoCompressionProgress(completedCount = index + 1, activeItemProgress = 0f))
            }
            results
        }
    }

    private suspend fun compressOne(
        video: MediaItem,
        profile: LargeVideoCompressionProfile,
        completedCount: Int,
        onProgress: (LargeVideoCompressionProgress) -> Unit,
    ): LargeVideoCompressionResult {
        return try {
            compressOne(
                video = video,
                profile = profile,
                completedCount = completedCount,
                removeAudio = false,
                onProgress = onProgress,
            )
        } catch (exception: LargeVideoCompressionExportException) {
            if (!exception.isUnsupportedPcmAudio()) throw exception
            Log.w(
                TAG,
                "Retrying ${video.displayName} as video-only because the source audio uses unsupported PCM.",
                exception,
            )
            compressOne(
                video = video,
                profile = profile,
                completedCount = completedCount,
                removeAudio = true,
                onProgress = onProgress,
            ).copy(audioRemoved = true)
        }
    }

    private suspend fun compressOne(
        video: MediaItem,
        profile: LargeVideoCompressionProfile,
        completedCount: Int,
        removeAudio: Boolean,
        onProgress: (LargeVideoCompressionProgress) -> Unit,
    ): LargeVideoCompressionResult {
        val inputUri = video.contentUri ?: error("Video has no readable content URI")
        val outputFile = createOutputFile(video, profile, removeAudio)
        return suspendCancellableCoroutine { continuation ->
            val mainHandler = Handler(Looper.getMainLooper())
            val progressHolder = ProgressHolder()
            lateinit var transformer: Transformer
            val progressPoller = object : Runnable {
                override fun run() {
                    if (!continuation.isActive) return
                    val progressState = transformer.getProgress(progressHolder)
                    if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(
                            LargeVideoCompressionProgress(
                                completedCount = completedCount,
                                activeItemProgress = progressHolder.progress / 100f,
                            ),
                        )
                    }
                    if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                        mainHandler.postDelayed(this, PROGRESS_POLL_INTERVAL_MILLIS)
                    }
                }
            }
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    mainHandler.removeCallbacks(progressPoller)
                    if (continuation.isActive) {
                        try {
                            val result = publishVerifiedOutput(
                                source = outputFile,
                                sourceVideo = video,
                                profile = profile,
                                removeAudio = removeAudio,
                            )
                            continuation.resume(result)
                        } catch (exception: Exception) {
                            outputFile.delete()
                            continuation.resumeWithException(exception)
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    mainHandler.removeCallbacks(progressPoller)
                    outputFile.delete()
                    if (continuation.isActive) {
                        Log.e(
                            TAG,
                            "Large video compression failed. code=${exportException.errorCodeName}, codec=${exportException.codecInfo}, video=${video.displayName}",
                            exportException,
                        )
                        continuation.resumeWithException(
                            LargeVideoCompressionExportException(exportException),
                        )
                    }
                }
            }
            transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(listener)
                .build()
            continuation.invokeOnCancellation {
                mainHandler.removeCallbacks(progressPoller)
                transformer.cancel()
                outputFile.delete()
            }
            val editedMediaItem = EditedMediaItem.Builder(Media3MediaItem.fromUri(inputUri))
                .setRemoveAudio(removeAudio)
                .setEffects(Effects(emptyList(), listOf(profile.presentationEffect(video.height))))
                .build()
            transformer.start(editedMediaItem, outputFile.absolutePath)
            mainHandler.post(progressPoller)
        }
    }

    private fun createOutputFile(
        video: MediaItem,
        profile: LargeVideoCompressionProfile,
        removeAudio: Boolean,
    ): File {
        val outputDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
            "AI Photo Cleaner/Compressed",
        )
        outputDirectory.mkdirs()
        val baseName = video.displayName.substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .ifBlank { "video" }
        val outputFile = File(
            outputDirectory,
            "${baseName}_${profile.name.lowercase()}${if (removeAudio) "_video_only" else ""}_${System.currentTimeMillis()}.mp4",
        )
        if (outputFile.exists()) outputFile.delete()
        return outputFile
    }

    private fun publishVerifiedOutput(
        source: File,
        sourceVideo: MediaItem,
        profile: LargeVideoCompressionProfile,
        removeAudio: Boolean,
    ): LargeVideoCompressionResult {
        val sourceBytes = source.length().coerceAtLeast(0L)
        if (sourceBytes <= 0L) {
            throw IOException("Compression output was empty.")
        }
        if (sourceBytes >= sourceVideo.sizeBytes) {
            throw IOException("Compression output was not smaller than the original.")
        }
        val publishedUri = publishToUserMovies(
            source = source,
            sourceVideo = sourceVideo,
            profile = profile,
            removeAudio = removeAudio,
        )
        val verification = LargeVideoOutputVerification.verify(
            originalBytes = sourceVideo.sizeBytes,
            outputBytes = sourceBytes,
            outputUri = publishedUri.toString(),
        )
        if (verification != LargeVideoOutputVerification.Valid) {
            cleanupPublishedUri(publishedUri)
            throw IOException("Compression output failed verification: $verification")
        }
        source.delete()
        return LargeVideoCompressionResult(
            sourceId = sourceVideo.id,
            outputPath = publishedUri.toString(),
            originalBytes = sourceVideo.sizeBytes,
            outputBytes = sourceBytes,
        )
    }

    private fun publishToUserMovies(
        source: File,
        sourceVideo: MediaItem,
        profile: LargeVideoCompressionProfile,
        removeAudio: Boolean,
    ): Uri {
        val resolver = context.contentResolver
        val displayName = buildOutputDisplayName(sourceVideo, profile, removeAudio)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AI Photo Cleaner")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create a public Movies output file.")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Could not open the public Movies output file.")
            val publishedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, publishedValues, null, null)
            return uri
        } catch (exception: Exception) {
            cleanupPublishedUri(uri)
            throw exception
        }
    }

    private fun cleanupPublishedUri(uri: Uri) {
        runCatching {
            context.contentResolver.delete(uri, null, null)
        }
    }

    private fun buildOutputDisplayName(
        video: MediaItem,
        profile: LargeVideoCompressionProfile,
        removeAudio: Boolean,
    ): String {
        val baseName = video.displayName.substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .ifBlank { "video" }
        return "${baseName}_${profile.name.lowercase()}${if (removeAudio) "_video_only" else ""}_${System.currentTimeMillis()}.mp4"
    }

    private fun LargeVideoCompressionProfile.presentationEffect(sourceHeight: Int?): Effect {
        val profileHeight = when (this) {
            LargeVideoCompressionProfile.StorageSaver -> 540
            LargeVideoCompressionProfile.Balanced -> 720
            LargeVideoCompressionProfile.HighQuality -> 1080
        }
        val targetHeight = sourceHeight?.coerceAtMost(profileHeight) ?: profileHeight
        return Presentation.createForHeight(targetHeight.coerceAtLeast(144))
    }

    private fun buildExportUserMessage(exception: ExportException): String {
        val causeMessage = exception.cause?.message?.takeIf { it.isNotBlank() }
        return buildString {
            append(exception.getErrorCodeName())
            if (causeMessage != null) {
                append(": ")
                append(causeMessage)
            }
        }
    }

    private inner class LargeVideoCompressionExportException(
        val exportException: ExportException,
    ) : Exception(buildExportUserMessage(exportException), exportException)

    private fun LargeVideoCompressionExportException.isUnsupportedPcmAudio(): Boolean {
        return exportException.errorCode == ExportException.ERROR_CODE_FAILED_RUNTIME_CHECK && (
            exportException.hasCauseMessage("Unsupported PCM encoding") ||
            exportException.hasCauseMessage("encoding=21")
            )
    }

    private fun Throwable.hasCauseMessage(pattern: String): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current.message?.contains(pattern, ignoreCase = true) == true) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private companion object {
        const val TAG = "LargeVideoCompressor"
        const val PROGRESS_POLL_INTERVAL_MILLIS = 500L
    }
}
