package com.air.cleaner.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.DuplicatePhotoDetector
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMediaStoreRepository(
    private val contentResolver: ContentResolver,
    private val similarScreenshotFingerprintCache: PerceptualFingerprintCache = InMemoryPerceptualFingerprintCache(),
    private val similarScreenshotResultCache: SimilarScreenshotResultCache = InMemorySimilarScreenshotResultCache(),
) : MediaRepository {
    override suspend fun scanSummary(): MediaScanSummary = withContext(Dispatchers.IO) {
        val accumulator = MediaScanSummaryAccumulator()
        scanItems(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.Image,
        ).forEach { itemWithPath ->
            accumulator.add(itemWithPath.item, itemWithPath.relativePath)
        }
        scanItems(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.Video,
        ).forEach { itemWithPath ->
            accumulator.add(itemWithPath.item, itemWithPath.relativePath)
        }
        accumulator.summary()
    }

    override suspend fun scanDuplicatePhotoGroups(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        ExactDuplicatePhotoScanner(
            contentFingerprint = { contentKey ->
                runCatching {
                    contentResolver.openInputStream(Uri.parse(contentKey))?.use { inputStream ->
                        ImageContentFingerprinter.sha256DecodedPixels(inputStream)
                    }
                }.getOrNull()
            },
        ).findDuplicateGroups(
            scanImageItems().map { itemWithPath ->
                DuplicatePhotoCandidate(
                    item = itemWithPath.item,
                    contentKey = itemWithPath.contentUri.toString(),
                )
            },
        )
    }

    override suspend fun scanSimilarScreenshotGroupResult(): SimilarScreenshotScanResult = withContext(Dispatchers.IO) {
        var fingerprintCacheHitCount = 0
        var fingerprintCacheMissCount = 0
        val result = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                val cacheResult = similarScreenshotFingerprintCache.getOrPutResult(candidate.fingerprintCacheKey) {
                    runCatching {
                        contentResolver.openInputStream(Uri.parse(candidate.contentKey))?.use { inputStream ->
                            ImageContentFingerprinter.averageHash(inputStream)
                        }
                    }.getOrNull()
                }
                if (cacheResult.cacheHit) {
                    fingerprintCacheHitCount += 1
                } else {
                    fingerprintCacheMissCount += 1
                }
                cacheResult.fingerprint
            },
        ).findSimilarGroupResult(
            scanImageItems().map { itemWithPath ->
                SimilarScreenshotCandidate(
                    item = itemWithPath.item,
                    contentKey = itemWithPath.contentUri.toString(),
                    relativePath = itemWithPath.relativePath,
                    cacheDateMillis = itemWithPath.dateModifiedMillis ?: itemWithPath.item.dateTakenMillis,
                )
            },
        )
        result.copy(
            fingerprintCacheHitCount = fingerprintCacheHitCount,
            fingerprintCacheMissCount = fingerprintCacheMissCount,
        )
    }

    override suspend fun cachedSimilarScreenshotGroups(): List<DuplicateGroup> = cachedSimilarScreenshotGroupResult().groups

    override suspend fun cachedSimilarScreenshotGroupResult(): CachedDuplicateGroupsResult = withContext(Dispatchers.IO) {
        ContentUriPresenceVerifier(
            exists = { contentUri -> contentUriExists(contentUri) },
        ).stillPresentGroupResult(similarScreenshotResultCache.load())
    }

    override suspend fun saveSimilarScreenshotGroups(groups: List<DuplicateGroup>) = withContext(Dispatchers.IO) {
        similarScreenshotResultCache.save(groups)
    }

    override suspend fun contentUrisStillPresent(contentUris: List<String>): List<String> = withContext(Dispatchers.IO) {
        ContentUriPresenceVerifier(
            exists = { contentUri -> contentUriExists(contentUri) },
        ).stillPresent(contentUris)
    }

    private fun scanImageItems(): List<MediaItemWithPath> {
        return scanItems(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.Image,
        )
    }

    private fun scanVideoItems(): List<MediaItemWithPath> {
        return scanItems(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.Video,
        )
    }

    private fun contentUriExists(contentUri: String): Boolean {
        return runCatching {
            contentResolver.query(
                Uri.parse(contentUri),
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null,
            )?.use { cursor -> cursor.moveToFirst() } == true
        }.getOrDefault(false)
    }

    private fun scanItems(
        uri: Uri,
        mediaType: MediaType,
    ): List<MediaItemWithPath> {
        val items = mutableListOf<MediaItemWithPath>()
        contentResolver.query(
            uri,
            projection(),
            "${MediaStore.MediaColumns.SIZE} > 0",
            null,
            "${MediaStore.MediaColumns.SIZE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val row = cursor.toMediaStoreRow()
                val item = MediaStoreRowMapper.map(row, mediaType)
                if (item != null) {
                    val contentUri = ContentUris.withAppendedId(uri, row.id)
                    items += MediaItemWithPath(
                        item = item.copy(
                            contentUri = contentUri.toString(),
                        ),
                        relativePath = row.relativePath,
                        contentUri = contentUri,
                        dateModifiedMillis = row.dateModifiedSeconds?.times(1_000L),
                    )
                }
            }
        }
        return items
    }

    private fun projection(): Array<String> {
        return buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            if (Build.VERSION.SDK_INT >= 29) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()
    }

    private fun Cursor.toMediaStoreRow(): MediaStoreRow {
        return MediaStoreRow(
            id = getRequiredLong(MediaStore.MediaColumns._ID),
            displayName = getOptionalString(MediaStore.MediaColumns.DISPLAY_NAME),
            sizeBytes = getOptionalLong(MediaStore.MediaColumns.SIZE),
            dateTakenMillis = getOptionalLong(MediaStore.MediaColumns.DATE_TAKEN),
            dateModifiedSeconds = getOptionalLong(MediaStore.MediaColumns.DATE_MODIFIED),
            relativePath = if (Build.VERSION.SDK_INT >= 29) {
                getOptionalString(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                null
            },
            width = getOptionalInt(MediaStore.MediaColumns.WIDTH),
            height = getOptionalInt(MediaStore.MediaColumns.HEIGHT),
        )
    }

    private fun Cursor.getRequiredLong(columnName: String): Long {
        return getLong(getColumnIndexOrThrow(columnName))
    }

    private fun Cursor.getOptionalLong(columnName: String): Long? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private fun Cursor.getOptionalInt(columnName: String): Int? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getInt(index) else null
    }

    private fun Cursor.getOptionalString(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private data class MediaItemWithPath(
        val item: MediaItem,
        val relativePath: String?,
        val contentUri: Uri,
        val dateModifiedMillis: Long?,
    )
}
