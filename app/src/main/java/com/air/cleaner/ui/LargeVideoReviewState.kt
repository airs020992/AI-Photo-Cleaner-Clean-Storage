package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.feature.photos.PhotoDeletionResult
import com.air.cleaner.feature.photos.PhotoDeletionStatus
import com.air.cleaner.feature.photos.PhotoDeletionSummary

internal data class LargeVideoReviewSelectionState(
    val videos: List<MediaItem>,
    private val selectedIds: Set<String>,
) {
    val selectedCount: Int = videos.count { video -> video.id in selectedIds }
    val selectedBytes: Long = videos
        .filter { video -> video.id in selectedIds }
        .sumOf { video -> video.sizeBytes }
    val selectedItems: List<MediaItem> = videos.filter { video -> video.id in selectedIds }

    fun isSelected(id: String): Boolean = id in selectedIds

    fun toggle(id: String): LargeVideoReviewSelectionState {
        return copy(
            selectedIds = if (id in selectedIds) {
                selectedIds - id
            } else {
                selectedIds + id
            },
        )
    }

    fun clearSelection(): LargeVideoReviewSelectionState {
        return copy(selectedIds = emptySet())
    }

    fun selectAll(): LargeVideoReviewSelectionState {
        return copy(selectedIds = videos.map { video -> video.id }.toSet())
    }

    fun deleteSummary(): PhotoDeletionSummary = PhotoDeletionSummary.fromItems(selectedItems)

    fun compressionEstimate(
        profile: LargeVideoCompressionProfile = LargeVideoCompressionProfile.Balanced,
        formatBytes: (Long) -> String,
    ): LargeVideoCompressionEstimateState {
        val originalBytes = selectedBytes
        if (originalBytes <= 0L) {
            return LargeVideoCompressionEstimateState(
                profile = profile,
                profileLabel = profile.label,
                originalBytes = 0L,
                estimatedCompressedBytes = 0L,
                estimatedSavingsBytes = 0L,
                estimatedCompressedLabel = formatBytes(0L),
                estimatedSavingsLabel = formatBytes(0L),
                estimatedSavingsRatioLabel = "~0%",
                guidance = "Select videos to preview compression savings.",
            )
        }
        val estimatedCompressedBytes = originalBytes * profile.compressedPercent / 100L
        val estimatedSavingsBytes = originalBytes - estimatedCompressedBytes
        return LargeVideoCompressionEstimateState(
            profile = profile,
            profileLabel = profile.label,
            originalBytes = originalBytes,
            estimatedCompressedBytes = estimatedCompressedBytes,
            estimatedSavingsBytes = estimatedSavingsBytes,
            estimatedCompressedLabel = formatBytes(estimatedCompressedBytes),
            estimatedSavingsLabel = formatBytes(estimatedSavingsBytes),
            estimatedSavingsRatioLabel = "~${100 - profile.compressedPercent}%",
            guidance = "Estimate only. Compression keeps a smaller copy; deletion still requires Android confirmation.",
        )
    }

    fun compressionProfilePreviews(
        selectedProfile: LargeVideoCompressionProfile,
        formatBytes: (Long) -> String,
    ): List<LargeVideoCompressionProfilePreviewState> {
        return LargeVideoCompressionProfile.entries.map { profile ->
            val estimate = compressionEstimate(profile = profile, formatBytes = formatBytes)
            LargeVideoCompressionProfilePreviewState(
                profile = profile,
                title = profile.label,
                subtitle = profile.subtitle,
                estimatedCompressedLabel = estimate.estimatedCompressedLabel,
                estimatedSavingsLabel = "${estimate.estimatedSavingsLabel} saved",
                selected = profile == selectedProfile,
            )
        }
    }

    fun rows(formatBytes: (Long) -> String): List<LargeVideoReviewRowState> {
        return videos.map { video ->
            LargeVideoReviewRowState(
                id = video.id,
                title = video.displayName,
                subtitle = "${video.durationLabel()} | ${formatBytes(video.sizeBytes)}",
                location = video.relativePath ?: "On device",
                metric = formatBytes(video.sizeBytes),
                selected = isSelected(video.id),
                contentUri = video.contentUri,
            )
        }
    }

    companion object {
        fun fromVideos(videos: List<MediaItem>): LargeVideoReviewSelectionState {
            return LargeVideoReviewSelectionState(
                videos = videos,
                selectedIds = videos.map { video -> video.id }.toSet(),
            )
        }
    }
}

internal enum class LargeVideoCompressionProfile(
    val label: String,
    val subtitle: String,
    val compressedPercent: Long,
) {
    StorageSaver(
        label = "Storage saver",
        subtitle = "Smallest file",
        compressedPercent = 35L,
    ),
    Balanced(
        label = "Balanced",
        subtitle = "Recommended",
        compressedPercent = 50L,
    ),
    HighQuality(
        label = "High quality",
        subtitle = "Keeps more detail",
        compressedPercent = 70L,
    ),
}

internal data class LargeVideoCompressionEstimateState(
    val profile: LargeVideoCompressionProfile,
    val profileLabel: String,
    val originalBytes: Long,
    val estimatedCompressedBytes: Long,
    val estimatedSavingsBytes: Long,
    val estimatedCompressedLabel: String,
    val estimatedSavingsLabel: String,
    val estimatedSavingsRatioLabel: String,
    val guidance: String,
)

internal data class LargeVideoCompressionProfilePreviewState(
    val profile: LargeVideoCompressionProfile,
    val title: String,
    val subtitle: String,
    val estimatedCompressedLabel: String,
    val estimatedSavingsLabel: String,
    val selected: Boolean,
)

internal data class LargeVideoCompressionAccessState(
    val canStartCompression: Boolean,
    val requiresUnlock: Boolean,
    val badgeLabel: String,
    val message: String,
) {
    companion object {
        fun fromSelection(
            selectedCount: Int,
            isPremium: Boolean,
            hasRewardedUnlock: Boolean,
            freePreviewLimit: Int = 1,
        ): LargeVideoCompressionAccessState {
            if (selectedCount <= 0) {
                return LargeVideoCompressionAccessState(
                    canStartCompression = false,
                    requiresUnlock = false,
                    badgeLabel = "Select videos",
                    message = "Select a large video to preview compression savings.",
                )
            }
            if (isPremium) {
                return LargeVideoCompressionAccessState(
                    canStartCompression = true,
                    requiresUnlock = false,
                    badgeLabel = "Unlimited",
                    message = "Premium includes unlimited large-video compression.",
                )
            }
            if (hasRewardedUnlock) {
                return LargeVideoCompressionAccessState(
                    canStartCompression = true,
                    requiresUnlock = false,
                    badgeLabel = "Reward unlocked",
                    message = "Reward unlock is active for this compression batch.",
                )
            }
            if (selectedCount <= freePreviewLimit) {
                return LargeVideoCompressionAccessState(
                    canStartCompression = true,
                    requiresUnlock = false,
                    badgeLabel = "Free preview",
                    message = "Compress 1 video free. Upgrade for unlimited large-video cleanup.",
                )
            }
            return LargeVideoCompressionAccessState(
                canStartCompression = false,
                requiresUnlock = true,
                badgeLabel = "Premium",
                message = "Batch compression is a premium cleanup. Select 1 video for a free preview or unlock unlimited compression.",
            )
        }
    }
}

internal enum class LargeVideoCompressionJobPhase {
    Idle,
    Running,
    Completed,
    Canceled,
    Failed,
}

internal data class LargeVideoCompressionJobState(
    val phase: LargeVideoCompressionJobPhase = LargeVideoCompressionJobPhase.Idle,
    val profile: LargeVideoCompressionProfile = LargeVideoCompressionProfile.Balanced,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val outputCount: Int = 0,
    val originalBytes: Long = 0L,
    val estimatedCompressedBytes: Long = 0L,
    val estimatedSavingsBytes: Long = 0L,
    val actualOutputBytes: Long = 0L,
    val actualSavingsBytes: Long = 0L,
    val progressFraction: Float? = null,
    val title: String = "Ready to compress",
    val subtitle: String = "Choose videos and a quality profile.",
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = when {
            totalCount <= 0 -> 0f
            phase == LargeVideoCompressionJobPhase.Completed -> 1f
            progressFraction != null -> progressFraction.coerceIn(0f, 1f)
            else -> completedCount.toFloat() / totalCount.toFloat()
        }

    val canStart: Boolean
        get() = phase != LargeVideoCompressionJobPhase.Running

    val canCancel: Boolean
        get() = phase == LargeVideoCompressionJobPhase.Running

    fun markItemCompleted(formatBytes: (Long) -> String): LargeVideoCompressionJobState {
        if (phase != LargeVideoCompressionJobPhase.Running) return this
        val nextCompletedCount = (completedCount + 1).coerceAtMost(totalCount)
        return if (nextCompletedCount >= totalCount) {
            copy(
                phase = LargeVideoCompressionJobPhase.Completed,
                completedCount = nextCompletedCount,
                progressFraction = null,
                title = "Compression pass complete",
                subtitle = "Estimated saved: ${formatBytes(estimatedSavingsBytes)}",
            )
        } else {
            copy(
                completedCount = nextCompletedCount,
                progressFraction = nextCompletedCount.toFloat() / totalCount.toFloat(),
                title = "Compressing $totalCount ${if (totalCount == 1) "video" else "videos"}",
                subtitle = "$nextCompletedCount of $totalCount complete",
            )
        }
    }

    fun updateProgress(
        completedItems: Int,
        activeItemProgress: Float,
    ): LargeVideoCompressionJobState {
        if (phase != LargeVideoCompressionJobPhase.Running || totalCount <= 0) return this
        val normalizedCompleted = completedItems.coerceIn(0, totalCount)
        val normalizedActiveProgress = activeItemProgress.coerceIn(0f, 1f)
        val aggregateProgress = ((normalizedCompleted + normalizedActiveProgress) / totalCount.toFloat())
            .coerceIn(0f, 0.99f)
        return copy(
            completedCount = normalizedCompleted,
            progressFraction = aggregateProgress,
            title = "Compressing $totalCount ${if (totalCount == 1) "video" else "videos"}",
            subtitle = "$normalizedCompleted of $totalCount complete",
        )
    }

    fun cancel(): LargeVideoCompressionJobState {
        return copy(
            phase = LargeVideoCompressionJobPhase.Canceled,
            title = "Compression canceled",
            subtitle = "Original videos were kept untouched.",
        )
    }

    fun fail(message: String): LargeVideoCompressionJobState {
        return copy(
            phase = LargeVideoCompressionJobPhase.Failed,
            title = "Compression failed",
            subtitle = "Original videos were kept untouched.",
            errorMessage = message,
        )
    }

    fun completeWithResults(
        results: List<LargeVideoCompressionResult>,
        formatBytes: (Long) -> String,
    ): LargeVideoCompressionJobState {
        val actualOriginalBytes = results.sumOf { result -> result.originalBytes }
        val outputBytes = results.sumOf { result -> result.outputBytes }
        val savingsBytes = (actualOriginalBytes - outputBytes).coerceAtLeast(0L)
        val audioRemovedCount = results.count { result -> result.audioRemoved }
        val audioNote = if (audioRemovedCount > 0) {
            " $audioRemovedCount source audio track was unsupported, so that smaller copy is video-only."
        } else {
            ""
        }
        return copy(
            phase = LargeVideoCompressionJobPhase.Completed,
            completedCount = results.size,
            outputCount = results.size,
            actualOutputBytes = outputBytes,
            actualSavingsBytes = savingsBytes,
            progressFraction = null,
            title = "Compression complete",
            subtitle = "Created ${results.size} ${if (results.size == 1) "smaller copy" else "smaller copies"}. Saved about ${formatBytes(savingsBytes)}.$audioNote",
            errorMessage = null,
        )
    }

    companion object {
        fun start(
            selection: LargeVideoReviewSelectionState,
            profile: LargeVideoCompressionProfile,
            formatBytes: (Long) -> String,
        ): LargeVideoCompressionJobState {
            if (selection.selectedCount <= 0) {
                return LargeVideoCompressionJobState()
            }
            val estimate = selection.compressionEstimate(profile = profile, formatBytes = formatBytes)
            return LargeVideoCompressionJobState(
                phase = LargeVideoCompressionJobPhase.Running,
                profile = profile,
                totalCount = selection.selectedCount,
                completedCount = 0,
                originalBytes = estimate.originalBytes,
                estimatedCompressedBytes = estimate.estimatedCompressedBytes,
                estimatedSavingsBytes = estimate.estimatedSavingsBytes,
                title = "Compressing ${selection.selectedCount} ${if (selection.selectedCount == 1) "video" else "videos"}",
                subtitle = "0 of ${selection.selectedCount} complete",
            )
        }
    }
}

internal data class LargeVideoCompressionResult(
    val sourceId: String,
    val outputPath: String,
    val originalBytes: Long,
    val outputBytes: Long,
    val audioRemoved: Boolean = false,
)

internal data class LargeVideoSourceCleanupState(
    val canRequestSourceDelete: Boolean,
    val actionLabel: String,
    val title: String,
    val guidance: String,
    val deleteSummary: PhotoDeletionSummary,
) {
    companion object {
        fun fromCompressionResults(
            selection: LargeVideoReviewSelectionState,
            results: List<LargeVideoCompressionResult>,
            formatBytes: (Long) -> String,
        ): LargeVideoSourceCleanupState {
            val selectedIds = selection.selectedItems.map { item -> item.id }.toSet()
            val validCompressedSourceIds = results
                .filter { result ->
                    LargeVideoOutputVerification.verify(
                        originalBytes = result.originalBytes,
                        outputBytes = result.outputBytes,
                        outputUri = result.outputPath,
                    ) == LargeVideoOutputVerification.Valid
                }
                .map { result -> result.sourceId }
                .toSet()
            val deleteSummary = selection.deleteSummary()
            val allSelectedCompressed = selectedIds.isNotEmpty() && validCompressedSourceIds.containsAll(selectedIds)
            return if (allSelectedCompressed) {
                val originalNoun = if (deleteSummary.itemCount == 1) "original" else "originals"
                LargeVideoSourceCleanupState(
                    canRequestSourceDelete = true,
                    actionLabel = "Delete originals",
                    title = "${deleteSummary.itemCount} $originalNoun ready for Android delete confirmation",
                    guidance = "Free another ${formatBytes(deleteSummary.bytesToDelete)} after confirming in Android.",
                    deleteSummary = deleteSummary,
                )
            } else {
                val guidance = if (selection.selectedCount == 0) {
                    "Select at least one video to create a smaller copy before deleting originals."
                } else if (selection.selectedCount == 1) {
                    "Delete originals unlocks after the selected video has a smaller public copy."
                } else {
                    "Delete originals unlocks after all ${selection.selectedCount} selected videos have smaller public copies."
                }
                LargeVideoSourceCleanupState(
                    canRequestSourceDelete = false,
                    actionLabel = "Delete originals",
                    title = "Originals are still protected",
                    guidance = guidance,
                    deleteSummary = deleteSummary,
                )
            }
        }
    }
}

internal data class LargeVideoReviewBottomActionState(
    val primaryLabel: String,
    val primaryEnabled: Boolean,
    val trustMessage: String,
) {
    companion object {
        fun fromCleanupState(
            cleanup: LargeVideoSourceCleanupState,
            compressionRunning: Boolean,
        ): LargeVideoReviewBottomActionState {
            val trustMessage = when {
                compressionRunning ->
                    "Compression is running. Keep this screen open until the smaller copy is verified."
                cleanup.canRequestSourceDelete ->
                    "Verified smaller copy exists. Android will ask before deleting originals."
                else ->
                    "Create smaller copies first. Original deletion unlocks only after verified output."
            }
            return LargeVideoReviewBottomActionState(
                primaryLabel = cleanup.actionLabel,
                primaryEnabled = cleanup.canRequestSourceDelete && !compressionRunning,
                trustMessage = trustMessage,
            )
        }
    }
}

internal data class LargeVideoCompressionOutputAuditState(
    val canTrustSourceDelete: Boolean,
    val checkedCount: Int,
    val validCount: Int,
    val invalidCount: Int,
    val savedBytes: Long,
    val savedRatioPercent: Int,
    val title: String,
    val summary: String,
    val guidance: String,
) {
    companion object {
        fun fromResults(
            results: List<LargeVideoCompressionResult>,
            formatBytes: (Long) -> String,
        ): LargeVideoCompressionOutputAuditState {
            if (results.isEmpty()) {
                return LargeVideoCompressionOutputAuditState(
                    canTrustSourceDelete = false,
                    checkedCount = 0,
                    validCount = 0,
                    invalidCount = 0,
                    savedBytes = 0L,
                    savedRatioPercent = 0,
                    title = "No compressed copies verified",
                    summary = "Compress at least one selected video before deleting originals.",
                    guidance = "Originals stay protected until compressed copies are verified.",
                )
            }

            val validCount = results.count { result ->
                LargeVideoOutputVerification.verify(
                    originalBytes = result.originalBytes,
                    outputBytes = result.outputBytes,
                    outputUri = result.outputPath,
                ) == LargeVideoOutputVerification.Valid
            }
            val checkedCount = results.size
            val invalidCount = checkedCount - validCount
            val originalBytes = results.sumOf { result -> result.originalBytes }
            val outputBytes = results.sumOf { result -> result.outputBytes }
            val savedBytes = (originalBytes - outputBytes).coerceAtLeast(0L)
            val savedRatioPercent = if (originalBytes > 0L) {
                (savedBytes * 100L / originalBytes).coerceIn(0L, 100L).toInt()
            } else {
                0
            }
            val allValid = invalidCount == 0
            val outputNoun = if (checkedCount == 1) "output is" else "outputs are"
            return if (allValid) {
                LargeVideoCompressionOutputAuditState(
                    canTrustSourceDelete = true,
                    checkedCount = checkedCount,
                    validCount = validCount,
                    invalidCount = invalidCount,
                    savedBytes = savedBytes,
                    savedRatioPercent = savedRatioPercent,
                    title = if (checkedCount == 1) "Compressed copy verified" else "Compressed copies verified",
                    summary = "$validCount/$checkedCount $outputNoun public and smaller. Saved ${formatBytes(savedBytes)} ($savedRatioPercent%).",
                    guidance = "Open the smaller copy from Movies/AI Photo Cleaner before deleting originals if you want to spot-check playback.",
                )
            } else {
                LargeVideoCompressionOutputAuditState(
                    canTrustSourceDelete = false,
                    checkedCount = checkedCount,
                    validCount = validCount,
                    invalidCount = invalidCount,
                    savedBytes = savedBytes,
                    savedRatioPercent = savedRatioPercent,
                    title = "Output check needs attention",
                    summary = "$validCount/$checkedCount outputs passed. Fix $invalidCount ${if (invalidCount == 1) "output" else "outputs"} before original deletion is trusted.",
                    guidance = "Originals stay protected until every compressed copy is public, non-empty, and smaller than the source.",
                )
            }
        }
    }
}

internal data class LargeVideoCompressedCopyOpenActionState(
    val enabled: Boolean,
    val label: String,
    val title: String,
    val subtitle: String,
    val outputUri: String?,
) {
    companion object {
        fun fromResults(results: List<LargeVideoCompressionResult>): LargeVideoCompressedCopyOpenActionState {
            val validResults = results.filter { result ->
                LargeVideoOutputVerification.verify(
                    originalBytes = result.originalBytes,
                    outputBytes = result.outputBytes,
                    outputUri = result.outputPath,
                ) == LargeVideoOutputVerification.Valid
            }
            val firstValidOutput = validResults.firstOrNull()
            return if (firstValidOutput != null) {
                LargeVideoCompressedCopyOpenActionState(
                    enabled = true,
                    label = "Open compressed copy",
                    title = if (validResults.size == 1) "Spot-check copy" else "Spot-check first copy",
                    subtitle = "Opens 1 of ${validResults.size} verified smaller ${if (validResults.size == 1) "copy" else "copies"} before deleting originals.",
                    outputUri = firstValidOutput.outputPath,
                )
            } else {
                LargeVideoCompressedCopyOpenActionState(
                    enabled = false,
                    label = "Open compressed copy",
                    title = "No playable copy ready",
                    subtitle = "Create a verified smaller public copy before spot-checking playback.",
                    outputUri = null,
                )
            }
        }
    }
}

internal data class LargeVideoPostDeleteStatus(
    val confirmedFreed: Boolean,
    val requestedCount: Int,
    val requestedBytes: Long,
    val stillPresentCount: Int,
    val title: String,
    val message: String,
    val requestedLabel: String,
    val freedLabel: String,
    val remainingLabel: String,
) {
    companion object {
        fun from(
            summary: PhotoDeletionSummary,
            result: PhotoDeletionResult,
            stillExistingContentUris: List<String>?,
            formatBytes: (Long) -> String,
        ): LargeVideoPostDeleteStatus {
            val requestedLabel = "${summary.itemCount} ${originalNoun(summary.itemCount)}"
            val stillPresentCount = when (result.status) {
                PhotoDeletionStatus.Deleted -> stillExistingContentUris.orEmpty().distinct().size
                PhotoDeletionStatus.Canceled,
                PhotoDeletionStatus.Blocked -> summary.itemCount
            }
            val remainingLabel = "$stillPresentCount still present"
            return when (result.status) {
                PhotoDeletionStatus.Deleted -> {
                    if (stillPresentCount == 0) {
                        LargeVideoPostDeleteStatus(
                            confirmedFreed = true,
                            requestedCount = summary.itemCount,
                            requestedBytes = summary.bytesToDelete,
                            stillPresentCount = 0,
                            title = "Originals removed",
                            message = "Android confirmed deletion and MediaStore no longer lists the selected originals.",
                            requestedLabel = requestedLabel,
                            freedLabel = formatBytes(summary.bytesToDelete),
                            remainingLabel = remainingLabel,
                        )
                    } else {
                        val presentNoun = originalNoun(stillPresentCount)
                        LargeVideoPostDeleteStatus(
                            confirmedFreed = false,
                            requestedCount = summary.itemCount,
                            requestedBytes = summary.bytesToDelete,
                            stillPresentCount = stillPresentCount,
                            title = "$stillPresentCount $presentNoun still present",
                            message = "Android returned success, but MediaStore still lists $stillPresentCount selected $presentNoun. Refresh or retry deletion before counting this space as freed.",
                            requestedLabel = requestedLabel,
                            freedLabel = "Not confirmed",
                            remainingLabel = remainingLabel,
                        )
                    }
                }
                PhotoDeletionStatus.Canceled -> LargeVideoPostDeleteStatus(
                    confirmedFreed = false,
                    requestedCount = summary.itemCount,
                    requestedBytes = summary.bytesToDelete,
                    stillPresentCount = summary.itemCount,
                    title = "Originals kept",
                    message = "Android deletion was canceled. No original video was removed.",
                    requestedLabel = requestedLabel,
                    freedLabel = formatBytes(0L),
                    remainingLabel = remainingLabel,
                )
                PhotoDeletionStatus.Blocked -> LargeVideoPostDeleteStatus(
                    confirmedFreed = false,
                    requestedCount = summary.itemCount,
                    requestedBytes = summary.bytesToDelete,
                    stillPresentCount = summary.itemCount,
                    title = "Delete confirmation unavailable",
                    message = "Android did not provide a delete confirmation flow. Original videos were kept.",
                    requestedLabel = requestedLabel,
                    freedLabel = formatBytes(0L),
                    remainingLabel = remainingLabel,
                )
            }
        }

        private fun originalNoun(count: Int): String {
            return if (count == 1) "original" else "originals"
        }
    }
}

internal enum class LargeVideoCompressionReleaseDecision {
    Ready,
    ReadyWithWarnings,
    Blocked,
}

internal data class LargeVideoCompressionQualityGate(
    val decision: LargeVideoCompressionReleaseDecision,
    val sampleCount: Int,
    val validOutputCount: Int,
    val blockerCount: Int,
    val warningCount: Int,
    val readinessPercent: Int,
    val originalBytes: Long,
    val outputBytes: Long,
    val savedBytes: Long,
    val savedRatioPercent: Int,
    val summary: String,
    val nextAction: String,
) {
    companion object {
        fun fromResults(results: List<LargeVideoCompressionResult>): LargeVideoCompressionQualityGate {
            val sampleCount = results.size
            val originalBytes = results.sumOf { result -> result.originalBytes }
            val outputBytes = results.sumOf { result -> result.outputBytes }
            val savedBytes = (originalBytes - outputBytes).coerceAtLeast(0L)
            val savedRatioPercent = if (originalBytes > 0L) {
                (savedBytes * 100L / originalBytes).coerceIn(0L, 100L).toInt()
            } else {
                0
            }
            val validOutputCount = results.count { result ->
                LargeVideoOutputVerification.verify(
                    originalBytes = result.originalBytes,
                    outputBytes = result.outputBytes,
                    outputUri = result.outputPath,
                ) == LargeVideoOutputVerification.Valid
            }
            val blockerCount = if (sampleCount == 0) {
                1
            } else {
                sampleCount - validOutputCount
            }
            val warningCount = results.count { result -> result.audioRemoved }
            val readinessPercent = if (sampleCount > 0) {
                validOutputCount * 100 / sampleCount
            } else {
                0
            }
            val decision = when {
                blockerCount > 0 -> LargeVideoCompressionReleaseDecision.Blocked
                warningCount > 0 -> LargeVideoCompressionReleaseDecision.ReadyWithWarnings
                else -> LargeVideoCompressionReleaseDecision.Ready
            }
            val summary = when (decision) {
                LargeVideoCompressionReleaseDecision.Blocked ->
                    if (sampleCount == 0) {
                        "0/0 outputs passed. Add at least one real compression sample before release."
                    } else {
                        "$validOutputCount/$sampleCount outputs passed. $blockerCount blocker outputs must be fixed before delete guidance is trusted."
                    }
                LargeVideoCompressionReleaseDecision.ReadyWithWarnings ->
                    "$validOutputCount/$sampleCount outputs are smaller and publicly visible. Saved $savedRatioPercent%. $warningCount video-only fallback needs audio coverage review."
                LargeVideoCompressionReleaseDecision.Ready ->
                    "$validOutputCount/$sampleCount outputs are smaller and publicly visible. Saved $savedRatioPercent%."
            }
            val nextAction = when (decision) {
                LargeVideoCompressionReleaseDecision.Blocked ->
                    if (sampleCount == 0) {
                        "Next: run a real-device compression sample and verify the public MediaStore output."
                    } else {
                        "Next: fix public MediaStore publishing and smaller-than-original verification before adding more formats."
                    }
                LargeVideoCompressionReleaseDecision.ReadyWithWarnings,
                LargeVideoCompressionReleaseDecision.Ready ->
                    "Next: expand the matrix with more device and app-origin videos before release."
            }
            return LargeVideoCompressionQualityGate(
                decision = decision,
                sampleCount = sampleCount,
                validOutputCount = validOutputCount,
                blockerCount = blockerCount,
                warningCount = warningCount,
                readinessPercent = readinessPercent,
                originalBytes = originalBytes,
                outputBytes = outputBytes,
                savedBytes = savedBytes,
                savedRatioPercent = savedRatioPercent,
                summary = summary,
                nextAction = nextAction,
            )
        }
    }
}

internal data class LargeVideoCompressionSampleEvidence(
    val sourceId: String,
    val sourceOrigin: String,
    val profile: LargeVideoCompressionProfile,
    val capturedAtMillis: Long = 0L,
    val result: LargeVideoCompressionResult,
)

internal data class LargeVideoCompressionSampleLedger(
    val samples: List<LargeVideoCompressionSampleEvidence> = emptyList(),
) {
    fun appendCompressionResults(
        results: List<LargeVideoCompressionResult>,
        profile: LargeVideoCompressionProfile,
        sourceVideos: List<MediaItem>,
        maxSamples: Int = 20,
        capturedAtMillis: Long = System.currentTimeMillis(),
    ): LargeVideoCompressionSampleLedger {
        val videosById = sourceVideos.associateBy { video -> video.id }
        val nextSamples = results.map { result ->
            LargeVideoCompressionSampleEvidence(
                sourceId = result.sourceId,
                sourceOrigin = videosById[result.sourceId].largeVideoSampleOrigin(),
                profile = profile,
                capturedAtMillis = capturedAtMillis,
                result = result,
            )
        }
        return copy(
            samples = (samples + nextSamples)
                .sortedBy { sample -> sample.capturedAtMillis }
                .takeLast(maxSamples.coerceAtLeast(1)),
        )
    }

    fun toSampleMatrix(
        requiredSampleCount: Int = 5,
        requiredOriginCount: Int = 2,
        requiredProfileCount: Int = 2,
    ): LargeVideoCompressionSampleMatrix {
        return LargeVideoCompressionSampleMatrix.fromSamples(
            samples = samples,
            requiredSampleCount = requiredSampleCount,
            requiredOriginCount = requiredOriginCount,
            requiredProfileCount = requiredProfileCount,
        )
    }
}

internal data class LargeVideoCompressionSampleMatrix(
    val decision: LargeVideoCompressionReleaseDecision,
    val sampleCount: Int,
    val originCount: Int,
    val profileCount: Int,
    val validOutputCount: Int,
    val blockerCount: Int,
    val videoOnlyCount: Int,
    val audioPreservedCount: Int,
    val readinessPercent: Int,
    val originalBytes: Long,
    val outputBytes: Long,
    val savedBytes: Long,
    val savedRatioPercent: Int,
    val summary: String,
    val nextAction: String,
) {
    companion object {
        fun fromCompressionResults(
            results: List<LargeVideoCompressionResult>,
            profile: LargeVideoCompressionProfile,
            sourceOrigin: String,
            requiredSampleCount: Int = 5,
            requiredOriginCount: Int = 2,
            requiredProfileCount: Int = 2,
        ): LargeVideoCompressionSampleMatrix {
            return fromSamples(
                samples = results.map { result ->
                    LargeVideoCompressionSampleEvidence(
                        sourceId = result.sourceId,
                        sourceOrigin = sourceOrigin,
                        profile = profile,
                        result = result,
                    )
                },
                requiredSampleCount = requiredSampleCount,
                requiredOriginCount = requiredOriginCount,
                requiredProfileCount = requiredProfileCount,
            )
        }

        fun fromSamples(
            samples: List<LargeVideoCompressionSampleEvidence>,
            requiredSampleCount: Int = 5,
            requiredOriginCount: Int = 2,
            requiredProfileCount: Int = 2,
        ): LargeVideoCompressionSampleMatrix {
            val sampleCount = samples.size
            val originCount = samples.map { sample -> sample.sourceOrigin }.distinct().size
            val profileCount = samples.map { sample -> sample.profile }.distinct().size
            val results = samples.map { sample -> sample.result }
            val originalBytes = results.sumOf { result -> result.originalBytes }
            val outputBytes = results.sumOf { result -> result.outputBytes }
            val savedBytes = (originalBytes - outputBytes).coerceAtLeast(0L)
            val savedRatioPercent = if (originalBytes > 0L) {
                (savedBytes * 100L / originalBytes).coerceIn(0L, 100L).toInt()
            } else {
                0
            }
            val validOutputCount = results.count { result ->
                LargeVideoOutputVerification.verify(
                    originalBytes = result.originalBytes,
                    outputBytes = result.outputBytes,
                    outputUri = result.outputPath,
                ) == LargeVideoOutputVerification.Valid
            }
            val invalidOutputCount = sampleCount - validOutputCount
            val videoOnlyCount = results.count { result -> result.audioRemoved }
            val audioPreservedCount = sampleCount - videoOnlyCount
            val coverageComplete = sampleCount >= requiredSampleCount &&
                originCount >= requiredOriginCount &&
                profileCount >= requiredProfileCount
            val hasAudioPreservedCoverage = audioPreservedCount > 0
            val blockerCount = invalidOutputCount + if (coverageComplete && hasAudioPreservedCoverage) 0 else 1
            val readinessPercent = percentOf(validOutputCount, requiredSampleCount)
            val decision = when {
                blockerCount > 0 -> LargeVideoCompressionReleaseDecision.Blocked
                videoOnlyCount > 0 -> LargeVideoCompressionReleaseDecision.ReadyWithWarnings
                else -> LargeVideoCompressionReleaseDecision.Ready
            }
            val summary = when (decision) {
                LargeVideoCompressionReleaseDecision.Blocked ->
                    "${validOutputCount}/${requiredSampleCount} required samples passed output checks across $originCount/$requiredOriginCount origins and $profileCount/$requiredProfileCount profiles. Saved $savedRatioPercent%. ${blockedCoverageMessage(sampleCount, requiredSampleCount, originCount, requiredOriginCount, profileCount, requiredProfileCount, hasAudioPreservedCoverage, invalidOutputCount)}"
                LargeVideoCompressionReleaseDecision.ReadyWithWarnings ->
                    "${validOutputCount}/${requiredSampleCount} required samples passed output checks across $originCount/$requiredOriginCount origins and $profileCount/$requiredProfileCount profiles. Saved $savedRatioPercent%. $videoOnlyCount video-only fallbacks need copy and format targeting before broad release."
                LargeVideoCompressionReleaseDecision.Ready ->
                    "${validOutputCount}/${requiredSampleCount} required samples passed output checks across $originCount/$requiredOriginCount origins and $profileCount/$requiredProfileCount profiles. Saved $savedRatioPercent%."
            }
            val nextAction = when (decision) {
                LargeVideoCompressionReleaseDecision.Blocked ->
                    nextBlockedMatrixAction(
                        sampleCount = sampleCount,
                        requiredSampleCount = requiredSampleCount,
                        profileCount = profileCount,
                        requiredProfileCount = requiredProfileCount,
                        hasAudioPreservedCoverage = hasAudioPreservedCoverage,
                        invalidOutputCount = invalidOutputCount,
                    )
                LargeVideoCompressionReleaseDecision.ReadyWithWarnings ->
                    "Next: keep video-only copy explicit and add audio-compatible format targeting."
                LargeVideoCompressionReleaseDecision.Ready ->
                    "Next: run deletion confirmation and playback spot checks before release."
            }
            return LargeVideoCompressionSampleMatrix(
                decision = decision,
                sampleCount = sampleCount,
                originCount = originCount,
                profileCount = profileCount,
                validOutputCount = validOutputCount,
                blockerCount = blockerCount,
                videoOnlyCount = videoOnlyCount,
                audioPreservedCount = audioPreservedCount,
                readinessPercent = readinessPercent,
                originalBytes = originalBytes,
                outputBytes = outputBytes,
                savedBytes = savedBytes,
                savedRatioPercent = savedRatioPercent,
                summary = summary,
                nextAction = nextAction,
            )
        }

        private fun percentOf(value: Int, required: Int): Int {
            if (required <= 0) return 100
            return (value * 100 / required).coerceIn(0, 100)
        }

        private fun blockedCoverageMessage(
            sampleCount: Int,
            requiredSampleCount: Int,
            originCount: Int,
            requiredOriginCount: Int,
            profileCount: Int,
            requiredProfileCount: Int,
            hasAudioPreservedCoverage: Boolean,
            invalidOutputCount: Int,
        ): String {
            return when {
                invalidOutputCount > 0 -> "$invalidOutputCount output checks failed."
                !hasAudioPreservedCoverage -> "Audio-preserved coverage is still missing."
                sampleCount < requiredSampleCount ->
                    "Sample coverage is short by ${requiredSampleCount - sampleCount} videos."
                originCount < requiredOriginCount ->
                    "Origin coverage is short by ${requiredOriginCount - originCount} source."
                profileCount < requiredProfileCount ->
                    "Profile coverage is short by ${requiredProfileCount - profileCount} profile."
                else -> "Coverage is incomplete."
            }
        }

        private fun nextBlockedMatrixAction(
            sampleCount: Int,
            requiredSampleCount: Int,
            profileCount: Int,
            requiredProfileCount: Int,
            hasAudioPreservedCoverage: Boolean,
            invalidOutputCount: Int,
        ): String {
            if (invalidOutputCount > 0) {
                return "Next: fix failed output verification before expanding the sample matrix."
            }
            val remainingSamples = (requiredSampleCount - sampleCount).coerceAtLeast(0)
            val samplePhrase = if (remainingSamples > 0) {
                "run at least $remainingSamples more real-device ${if (remainingSamples == 1) "video" else "videos"}"
            } else {
                "run the remaining coverage cases"
            }
            val profilePhrase = if (profileCount < requiredProfileCount) {
                " and another compression profile"
            } else {
                ""
            }
            val audioPhrase = if (!hasAudioPreservedCoverage) {
                ", including one audio-preserved output"
            } else {
                ""
            }
            return "Next: $samplePhrase$audioPhrase$profilePhrase."
        }
    }
}

internal enum class LargeVideoOutputVerification {
    Valid,
    EmptyOutput,
    NotSmallerThanOriginal,
    NotUserVisible,
    ;

    companion object {
        fun verify(
            originalBytes: Long,
            outputBytes: Long,
            outputUri: String,
        ): LargeVideoOutputVerification {
            return when {
                outputBytes <= 0L -> EmptyOutput
                outputBytes >= originalBytes -> NotSmallerThanOriginal
                !outputUri.startsWith("content://", ignoreCase = true) -> NotUserVisible
                else -> Valid
            }
        }
    }
}

internal data class LargeVideoReviewRowState(
    val id: String,
    val title: String,
    val subtitle: String,
    val location: String,
    val metric: String,
    val selected: Boolean,
    val contentUri: String?,
)

internal fun MediaItem.durationLabel(): String {
    val totalSeconds = (durationMillis ?: 0L).coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun MediaItem?.largeVideoSampleOrigin(): String {
    val path = this?.relativePath?.trim('/').orEmpty()
    val normalized = path.lowercase()
    return when {
        normalized.contains("aiphotocleanerrealsamples/") ->
            normalized.substringAfter("aiphotocleanerrealsamples/")
                .substringBefore('/')
                .ifBlank { "real_samples" }
        normalized.contains("aiphotocleanerlargevideosamples/") ->
            normalized.substringAfter("aiphotocleanerlargevideosamples/")
                .substringBefore('/')
                .ifBlank { "large_video_samples" }
        normalized.startsWith("dcim/camera") -> "camera"
        normalized.startsWith("movies") -> "movies"
        normalized.startsWith("pictures") -> "pictures"
        else -> "device_library"
    }
}
