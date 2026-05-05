package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import com.air.cleaner.feature.photos.PhotoDeletionResult
import com.air.cleaner.feature.photos.PhotoDeletionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeVideoReviewStateTest {
    @Test
    fun selectsAllVideosByDefaultForReview() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 720_000_000L, 75_000L),
            ),
        )

        assertEquals(2, state.selectedCount)
        assertEquals(3_120_000_000L, state.selectedBytes)
        assertTrue(state.isSelected("travel.mov"))
        assertTrue(state.isSelected("screen.mp4"))
    }

    @Test
    fun togglesOneVideoWithoutChangingOthers() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 720_000_000L, 75_000L),
            ),
        )

        val next = state.toggle("travel.mov")

        assertEquals(1, next.selectedCount)
        assertEquals(720_000_000L, next.selectedBytes)
        assertFalse(next.isSelected("travel.mov"))
        assertTrue(next.isSelected("screen.mp4"))
    }

    @Test
    fun clearsSelectionForControlledDeletes() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 720_000_000L, 75_000L),
            ),
        )

        val next = state.clearSelection()

        assertEquals(0, next.selectedCount)
        assertEquals(0L, next.selectedBytes)
        assertFalse(next.isSelected("travel.mov"))
        assertFalse(next.isSelected("screen.mp4"))
    }

    @Test
    fun selectsAllVideosAfterManualChanges() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 720_000_000L, 75_000L),
            ),
        ).clearSelection()

        val next = state.selectAll()

        assertEquals(2, next.selectedCount)
        assertEquals(3_120_000_000L, next.selectedBytes)
        assertTrue(next.isSelected("travel.mov"))
        assertTrue(next.isSelected("screen.mp4"))
    }

    @Test
    fun buildsRowsWithDurationSizeAndSelection() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
            ),
        )

        val rows = state.rows(formatBytes = { bytes -> "${bytes / 1_000_000} MB" })

        assertEquals(
            listOf(
                LargeVideoReviewRowState(
                    id = "travel.mov",
                    title = "Travel.mov",
                    subtitle = "3:05 | 2400 MB",
                    location = "Movies/Trips/",
                    metric = "2400 MB",
                    selected = true,
                    contentUri = "content://video/travel.mov",
                ),
            ),
            rows,
        )
    }

    @Test
    fun deleteSummaryUsesOnlySelectedVideos() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 720_000_000L, 75_000L),
            ),
        ).toggle("travel.mov")

        val summary = state.deleteSummary()

        assertEquals(1, summary.itemCount)
        assertEquals(720_000_000L, summary.bytesToDelete)
        assertEquals(listOf("content://video/screen.mp4"), summary.contentUris)
    }

    @Test
    fun estimatesCompressionSavingsForSelectedVideos() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 720_000_000L, 75_000L),
            ),
        ).toggle("screen.mp4")

        val estimate = state.compressionEstimate(formatBytes = { bytes -> "${bytes / 1_000_000} MB" })

        assertEquals(2_400_000_000L, estimate.originalBytes)
        assertEquals(1_200_000_000L, estimate.estimatedCompressedBytes)
        assertEquals(1_200_000_000L, estimate.estimatedSavingsBytes)
        assertEquals("1200 MB", estimate.estimatedCompressedLabel)
        assertEquals("1200 MB", estimate.estimatedSavingsLabel)
        assertEquals("~50%", estimate.estimatedSavingsRatioLabel)
        assertEquals(
            "Estimate only. Compression keeps a smaller copy; deletion still requires Android confirmation.",
            estimate.guidance,
        )
    }

    @Test
    fun compressionEstimateHandlesEmptySelection() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_400_000_000L, 185_000L),
            ),
        ).clearSelection()

        val estimate = state.compressionEstimate(formatBytes = { bytes -> "${bytes / 1_000_000} MB" })

        assertEquals(0L, estimate.originalBytes)
        assertEquals(0L, estimate.estimatedCompressedBytes)
        assertEquals(0L, estimate.estimatedSavingsBytes)
        assertEquals("0 MB", estimate.estimatedCompressedLabel)
        assertEquals("0 MB", estimate.estimatedSavingsLabel)
        assertEquals("~0%", estimate.estimatedSavingsRatioLabel)
        assertEquals("Select videos to preview compression savings.", estimate.guidance)
    }

    @Test
    fun compressionEstimateUsesSelectedProfile() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )

        val storageSaver = state.compressionEstimate(
            profile = LargeVideoCompressionProfile.StorageSaver,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )
        val highQuality = state.compressionEstimate(
            profile = LargeVideoCompressionProfile.HighQuality,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals(700_000_000L, storageSaver.estimatedCompressedBytes)
        assertEquals(1_300_000_000L, storageSaver.estimatedSavingsBytes)
        assertEquals("~65%", storageSaver.estimatedSavingsRatioLabel)
        assertEquals("Storage saver", storageSaver.profileLabel)
        assertEquals(1_400_000_000L, highQuality.estimatedCompressedBytes)
        assertEquals(600_000_000L, highQuality.estimatedSavingsBytes)
        assertEquals("~30%", highQuality.estimatedSavingsRatioLabel)
        assertEquals("High quality", highQuality.profileLabel)
    }

    @Test
    fun buildsCompressionProfilePreviewsForSelection() {
        val state = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )

        val previews = state.compressionProfilePreviews(
            selectedProfile = LargeVideoCompressionProfile.Balanced,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals(3, previews.size)
        assertEquals(
            LargeVideoCompressionProfilePreviewState(
                profile = LargeVideoCompressionProfile.StorageSaver,
                title = "Storage saver",
                subtitle = "Smallest file",
                estimatedCompressedLabel = "700 MB",
                estimatedSavingsLabel = "1300 MB saved",
                selected = false,
            ),
            previews[0],
        )
        assertEquals(
            LargeVideoCompressionProfilePreviewState(
                profile = LargeVideoCompressionProfile.Balanced,
                title = "Balanced",
                subtitle = "Recommended",
                estimatedCompressedLabel = "1000 MB",
                estimatedSavingsLabel = "1000 MB saved",
                selected = true,
            ),
            previews[1],
        )
        assertEquals(
            LargeVideoCompressionProfilePreviewState(
                profile = LargeVideoCompressionProfile.HighQuality,
                title = "High quality",
                subtitle = "Keeps more detail",
                estimatedCompressedLabel = "1400 MB",
                estimatedSavingsLabel = "600 MB saved",
                selected = false,
            ),
            previews[2],
        )
    }

    @Test
    fun startsCompressionJobFromCurrentSelectionAndProfile() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 1_000_000_000L, 75_000L),
            ),
        ).toggle("screen.mp4")

        val job = LargeVideoCompressionJobState.start(
            selection = selection,
            profile = LargeVideoCompressionProfile.StorageSaver,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals(LargeVideoCompressionJobPhase.Running, job.phase)
        assertEquals(LargeVideoCompressionProfile.StorageSaver, job.profile)
        assertEquals(1, job.totalCount)
        assertEquals(0, job.completedCount)
        assertEquals(2_000_000_000L, job.originalBytes)
        assertEquals(700_000_000L, job.estimatedCompressedBytes)
        assertEquals(1_300_000_000L, job.estimatedSavingsBytes)
        assertEquals("Compressing 1 video", job.title)
        assertEquals("0 of 1 complete", job.subtitle)
        assertEquals(0f, job.progress)
        assertFalse(job.canStart)
        assertTrue(job.canCancel)
    }

    @Test
    fun compressionJobProgressesAndCompletesSafely() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 1_000_000_000L, 75_000L),
            ),
        )

        val running = LargeVideoCompressionJobState.start(
            selection = selection,
            profile = LargeVideoCompressionProfile.Balanced,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )
        val halfway = running.markItemCompleted(formatBytes = { bytes -> "${bytes / 1_000_000} MB" })
        val complete = halfway.markItemCompleted(formatBytes = { bytes -> "${bytes / 1_000_000} MB" })

        assertEquals(LargeVideoCompressionJobPhase.Running, halfway.phase)
        assertEquals(1, halfway.completedCount)
        assertEquals("1 of 2 complete", halfway.subtitle)
        assertEquals(0.5f, halfway.progress)
        assertEquals(LargeVideoCompressionJobPhase.Completed, complete.phase)
        assertEquals(2, complete.completedCount)
        assertEquals("Compression pass complete", complete.title)
        assertEquals("Estimated saved: 1500 MB", complete.subtitle)
        assertEquals(1f, complete.progress)
        assertTrue(complete.canStart)
        assertFalse(complete.canCancel)
    }

    @Test
    fun compressionJobCompletesWithActualOutputResults() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 1_000_000_000L, 75_000L),
            ),
        )

        val complete = LargeVideoCompressionJobState.start(
            selection = selection,
            profile = LargeVideoCompressionProfile.Balanced,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        ).completeWithResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "/storage/emulated/0/Movies/AI Photo Cleaner/travel-cleaner.mp4",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 820_000_000L,
                ),
                LargeVideoCompressionResult(
                    sourceId = "screen.mp4",
                    outputPath = "/storage/emulated/0/Movies/AI Photo Cleaner/screen-cleaner.mp4",
                    originalBytes = 1_000_000_000L,
                    outputBytes = 430_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals(LargeVideoCompressionJobPhase.Completed, complete.phase)
        assertEquals(2, complete.completedCount)
        assertEquals(2, complete.outputCount)
        assertEquals(1_250_000_000L, complete.actualOutputBytes)
        assertEquals(1_750_000_000L, complete.actualSavingsBytes)
        assertEquals("Compression complete", complete.title)
        assertEquals("Created 2 smaller copies. Saved about 1750 MB.", complete.subtitle)
        assertEquals(1f, complete.progress)
    }

    @Test
    fun compressionJobFailsWithoutClaimingOutputs() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )

        val failed = LargeVideoCompressionJobState.start(
            selection = selection,
            profile = LargeVideoCompressionProfile.Balanced,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        ).fail("Codec is not available")

        assertEquals(LargeVideoCompressionJobPhase.Failed, failed.phase)
        assertEquals(0, failed.outputCount)
        assertEquals(0L, failed.actualOutputBytes)
        assertEquals(0L, failed.actualSavingsBytes)
        assertEquals("Compression failed", failed.title)
        assertEquals("Original videos were kept untouched.", failed.subtitle)
        assertEquals("Codec is not available", failed.errorMessage)
    }

    @Test
    fun compressionJobExplainsVideoOnlyFallbackForUnsupportedAudio() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )

        val complete = LargeVideoCompressionJobState.start(
            selection = selection,
            profile = LargeVideoCompressionProfile.Balanced,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        ).completeWithResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "/storage/emulated/0/Movies/AI Photo Cleaner/travel-cleaner.mp4",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 600_000_000L,
                    audioRemoved = true,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals("Compression complete", complete.title)
        assertEquals(
            "Created 1 smaller copy. Saved about 1400 MB. 1 source audio track was unsupported, so that smaller copy is video-only.",
            complete.subtitle,
        )
    }

    @Test
    fun outputVerificationAcceptsSmallerPublicMovieCopy() {
        val verification = LargeVideoOutputVerification.verify(
            originalBytes = 890_000_000L,
            outputBytes = 35_111_080L,
            outputUri = "content://media/external/video/media/42",
        )

        assertEquals(LargeVideoOutputVerification.Valid, verification)
    }

    @Test
    fun outputVerificationRejectsEmptyOutput() {
        val verification = LargeVideoOutputVerification.verify(
            originalBytes = 890_000_000L,
            outputBytes = 0L,
            outputUri = "content://media/external/video/media/42",
        )

        assertEquals(LargeVideoOutputVerification.EmptyOutput, verification)
    }

    @Test
    fun outputVerificationRejectsOutputThatDoesNotSaveSpace() {
        val verification = LargeVideoOutputVerification.verify(
            originalBytes = 890_000_000L,
            outputBytes = 900_000_000L,
            outputUri = "content://media/external/video/media/42",
        )

        assertEquals(LargeVideoOutputVerification.NotSmallerThanOriginal, verification)
    }

    @Test
    fun outputVerificationRejectsPrivateOutputPath() {
        val verification = LargeVideoOutputVerification.verify(
            originalBytes = 890_000_000L,
            outputBytes = 35_111_080L,
            outputUri = "/sdcard/Android/data/com.air.cleaner/files/Movies/AI Photo Cleaner/Compressed/out.mp4",
        )

        assertEquals(LargeVideoOutputVerification.NotUserVisible, verification)
    }

    @Test
    fun qualityGateTreatsSmallerPublicCopiesAsReleaseReady() {
        val gate = LargeVideoCompressionQualityGate.fromResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "DSCF6876.MOV",
                    outputPath = "content://media/external/video/media/1000008842",
                    originalBytes = 933_536_256L,
                    outputBytes = 35_143_971L,
                    audioRemoved = true,
                ),
                LargeVideoCompressionResult(
                    sourceId = "screen-recording.mp4",
                    outputPath = "content://media/external/video/media/1000008843",
                    originalBytes = 420_000_000L,
                    outputBytes = 180_000_000L,
                    audioRemoved = false,
                ),
            ),
        )

        assertEquals(LargeVideoCompressionReleaseDecision.ReadyWithWarnings, gate.decision)
        assertEquals(2, gate.sampleCount)
        assertEquals(2, gate.validOutputCount)
        assertEquals(0, gate.blockerCount)
        assertEquals(1, gate.warningCount)
        assertEquals(100, gate.readinessPercent)
        assertEquals(1_353_536_256L, gate.originalBytes)
        assertEquals(215_143_971L, gate.outputBytes)
        assertEquals(1_138_392_285L, gate.savedBytes)
        assertEquals(84, gate.savedRatioPercent)
        assertEquals(
            "2/2 outputs are smaller and publicly visible. Saved 84%. 1 video-only fallback needs audio coverage review.",
            gate.summary,
        )
        assertEquals(
            "Next: expand the matrix with more device and app-origin videos before release.",
            gate.nextAction,
        )
    }

    @Test
    fun qualityGateBlocksPrivateOrNonSavingOutputs() {
        val gate = LargeVideoCompressionQualityGate.fromResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "private-output.mov",
                    outputPath = "/sdcard/Android/data/com.air.cleaner/files/out.mp4",
                    originalBytes = 500_000_000L,
                    outputBytes = 100_000_000L,
                ),
                LargeVideoCompressionResult(
                    sourceId = "larger-output.mov",
                    outputPath = "content://media/external/video/media/44",
                    originalBytes = 300_000_000L,
                    outputBytes = 320_000_000L,
                ),
            ),
        )

        assertEquals(LargeVideoCompressionReleaseDecision.Blocked, gate.decision)
        assertEquals(0, gate.validOutputCount)
        assertEquals(2, gate.blockerCount)
        assertEquals(0, gate.readinessPercent)
        assertEquals(
            "0/2 outputs passed. 2 blocker outputs must be fixed before delete guidance is trusted.",
            gate.summary,
        )
        assertEquals(
            "Next: fix public MediaStore publishing and smaller-than-original verification before adding more formats.",
            gate.nextAction,
        )
    }

    @Test
    fun qualityGateBlocksMissingSampleEvidence() {
        val gate = LargeVideoCompressionQualityGate.fromResults(results = emptyList())

        assertEquals(LargeVideoCompressionReleaseDecision.Blocked, gate.decision)
        assertEquals(0, gate.sampleCount)
        assertEquals(0, gate.validOutputCount)
        assertEquals(1, gate.blockerCount)
        assertEquals(0, gate.readinessPercent)
        assertEquals("0/0 outputs passed. Add at least one real compression sample before release.", gate.summary)
        assertEquals(
            "Next: run a real-device compression sample and verify the public MediaStore output.",
            gate.nextAction,
        )
    }

    @Test
    fun sampleMatrixBlocksSingleVideoOnlyRealDeviceSampleUntilCoverageExpands() {
        val matrix = LargeVideoCompressionSampleMatrix.fromSamples(
            samples = listOf(
                LargeVideoCompressionSampleEvidence(
                    sourceId = "DSCF6872.MOV",
                    sourceOrigin = "huanqiu",
                    profile = LargeVideoCompressionProfile.Balanced,
                    result = LargeVideoCompressionResult(
                        sourceId = "DSCF6872.MOV",
                        outputPath = "content://media/external/video/media/1000008844",
                        originalBytes = 959_290_368L,
                        outputBytes = 36_117_812L,
                        audioRemoved = true,
                    ),
                ),
            ),
        )

        assertEquals(LargeVideoCompressionReleaseDecision.Blocked, matrix.decision)
        assertEquals(1, matrix.sampleCount)
        assertEquals(1, matrix.originCount)
        assertEquals(1, matrix.profileCount)
        assertEquals(1, matrix.videoOnlyCount)
        assertEquals(0, matrix.audioPreservedCount)
        assertEquals(1, matrix.validOutputCount)
        assertEquals(96, matrix.savedRatioPercent)
        assertEquals(20, matrix.readinessPercent)
        assertEquals(
            "1/5 required samples passed output checks across 1/2 origins and 1/2 profiles. Saved 96%. Audio-preserved coverage is still missing.",
            matrix.summary,
        )
        assertEquals(
            "Next: run at least 4 more real-device videos, including one audio-preserved output and another compression profile.",
            matrix.nextAction,
        )
    }

    @Test
    fun sampleMatrixAllowsReleaseWithWarningsAfterCoverageAndAudioFallbackAreKnown() {
        val matrix = LargeVideoCompressionSampleMatrix.fromSamples(
            samples = listOf(
                compressionSample("DSCF6872.MOV", "huanqiu", LargeVideoCompressionProfile.Balanced, 959_290_368L, 36_117_812L, audioRemoved = true),
                compressionSample("DSCF6876.MOV", "huanqiu", LargeVideoCompressionProfile.Balanced, 933_536_256L, 35_143_971L, audioRemoved = true),
                compressionSample("DSCF7863.MOV", "pingquan", LargeVideoCompressionProfile.StorageSaver, 31_781_376L, 14_000_000L, audioRemoved = false),
                compressionSample("screen-recording.mp4", "device_screen_recording", LargeVideoCompressionProfile.HighQuality, 420_000_000L, 210_000_000L, audioRemoved = false),
                compressionSample("camera_clip.mp4", "camera", LargeVideoCompressionProfile.Balanced, 800_000_000L, 300_000_000L, audioRemoved = false),
            ),
        )

        assertEquals(LargeVideoCompressionReleaseDecision.ReadyWithWarnings, matrix.decision)
        assertEquals(5, matrix.sampleCount)
        assertEquals(4, matrix.originCount)
        assertEquals(3, matrix.profileCount)
        assertEquals(2, matrix.videoOnlyCount)
        assertEquals(3, matrix.audioPreservedCount)
        assertEquals(5, matrix.validOutputCount)
        assertEquals(0, matrix.blockerCount)
        assertEquals(100, matrix.readinessPercent)
        assertEquals(
            "5/5 required samples passed output checks across 4/2 origins and 3/2 profiles. Saved 81%. 2 video-only fallbacks need copy and format targeting before broad release.",
            matrix.summary,
        )
    }

    @Test
    fun sampleMatrixBuildsRuntimeEvidenceFromCompressionResults() {
        val matrix = LargeVideoCompressionSampleMatrix.fromCompressionResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "DSCF6872.MOV",
                    outputPath = "content://media/external/video/media/1000008844",
                    originalBytes = 959_290_368L,
                    outputBytes = 36_117_812L,
                    audioRemoved = true,
                ),
            ),
            profile = LargeVideoCompressionProfile.Balanced,
            sourceOrigin = "device_library",
        )

        assertEquals(1, matrix.sampleCount)
        assertEquals(1, matrix.originCount)
        assertEquals(1, matrix.profileCount)
        assertEquals(1, matrix.videoOnlyCount)
        assertEquals(0, matrix.audioPreservedCount)
        assertEquals(20, matrix.readinessPercent)
        assertEquals(LargeVideoCompressionReleaseDecision.Blocked, matrix.decision)
    }

    @Test
    fun compressionJobCanBeCanceledWithoutTouchingOriginals() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 1_000_000_000L, 75_000L),
            ),
        )

        val canceled = LargeVideoCompressionJobState.start(
            selection = selection,
            profile = LargeVideoCompressionProfile.HighQuality,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        ).markItemCompleted(formatBytes = { bytes -> "${bytes / 1_000_000} MB" })
            .cancel()

        assertEquals(LargeVideoCompressionJobPhase.Canceled, canceled.phase)
        assertEquals(1, canceled.completedCount)
        assertEquals("Compression canceled", canceled.title)
        assertEquals("Original videos were kept untouched.", canceled.subtitle)
        assertTrue(canceled.canStart)
        assertFalse(canceled.canCancel)
    }

    @Test
    fun sourceCleanupStaysLockedUntilEverySelectedVideoHasAValidCompressedCopy() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 1_000_000_000L, 75_000L),
            ),
        )

        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "content://media/external/video/media/42",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertFalse(cleanup.canRequestSourceDelete)
        assertEquals("Delete originals", cleanup.actionLabel)
        assertEquals(
            "Delete originals unlocks after all 2 selected videos have smaller public copies.",
            cleanup.guidance,
        )
    }

    @Test
    fun sourceCleanupUnlocksSystemDeleteForCompressedSelectedOriginals() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
                video("screen.mp4", "Screen recording.mp4", 1_000_000_000L, 75_000L),
            ),
        )

        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "content://media/external/video/media/42",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                ),
                LargeVideoCompressionResult(
                    sourceId = "screen.mp4",
                    outputPath = "content://media/external/video/media/43",
                    originalBytes = 1_000_000_000L,
                    outputBytes = 400_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertTrue(cleanup.canRequestSourceDelete)
        assertEquals("Delete originals", cleanup.actionLabel)
        assertEquals("2 originals ready for Android delete confirmation", cleanup.title)
        assertEquals("Free another 3000 MB after confirming in Android.", cleanup.guidance)
        assertEquals(selection.deleteSummary(), cleanup.deleteSummary)
    }

    @Test
    fun sourceCleanupBlocksInvalidCompressedCopies() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )

        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "/private/out.mp4",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertFalse(cleanup.canRequestSourceDelete)
        assertEquals("Delete originals", cleanup.actionLabel)
        assertEquals(
            "Delete originals unlocks after the selected video has a smaller public copy.",
            cleanup.guidance,
        )
    }

    @Test
    fun sourceCleanupUsesSingularCopyForOneVerifiedOriginal() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )

        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "content://media/external/video/media/42",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertTrue(cleanup.canRequestSourceDelete)
        assertEquals("1 original ready for Android delete confirmation", cleanup.title)
        assertEquals("Free another 2000 MB after confirming in Android.", cleanup.guidance)
    }

    @Test
    fun sourceCleanupUsesActionableGuidanceWhenNothingIsSelected() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        ).clearSelection()

        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = emptyList(),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertFalse(cleanup.canRequestSourceDelete)
        assertEquals("Originals are still protected", cleanup.title)
        assertEquals(
            "Select at least one video to create a smaller copy before deleting originals.",
            cleanup.guidance,
        )
    }

    @Test
    fun bottomActionKeepsDeleteVisibleButLockedUntilCompressionIsVerified() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )
        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = emptyList(),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        val action = LargeVideoReviewBottomActionState.fromCleanupState(
            cleanup = cleanup,
            compressionRunning = false,
        )

        assertEquals("Delete originals", action.primaryLabel)
        assertFalse(action.primaryEnabled)
        assertEquals(
            "Create smaller copies first. Original deletion unlocks only after verified output.",
            action.trustMessage,
        )
    }

    @Test
    fun bottomActionUnlocksDeleteAfterVerifiedCompressionOutput() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )
        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "content://media/external/video/media/42",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        val action = LargeVideoReviewBottomActionState.fromCleanupState(
            cleanup = cleanup,
            compressionRunning = false,
        )

        assertEquals("Delete originals", action.primaryLabel)
        assertTrue(action.primaryEnabled)
        assertEquals(
            "Verified smaller copy exists. Android will ask before deleting originals.",
            action.trustMessage,
        )
    }

    @Test
    fun bottomActionBlocksDeleteWhileCompressionIsRunningEvenWithOldResults() {
        val selection = LargeVideoReviewSelectionState.fromVideos(
            videos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L),
            ),
        )
        val cleanup = LargeVideoSourceCleanupState.fromCompressionResults(
            selection = selection,
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "content://media/external/video/media/42",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        val action = LargeVideoReviewBottomActionState.fromCleanupState(
            cleanup = cleanup,
            compressionRunning = true,
        )

        assertEquals("Delete originals", action.primaryLabel)
        assertFalse(action.primaryEnabled)
        assertEquals("Compression is running. Keep this screen open until the smaller copy is verified.", action.trustMessage)
    }

    @Test
    fun outputAuditSummarizesVerifiedCompressedCopies() {
        val audit = LargeVideoCompressionOutputAuditState.fromResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "PXL_20260505_133027812.TS.mp4",
                    outputPath = "content://media/external/video/media/1000009001",
                    originalBytes = 461_758_049L,
                    outputBytes = 33_459_791L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertTrue(audit.canTrustSourceDelete)
        assertEquals(1, audit.checkedCount)
        assertEquals(1, audit.validCount)
        assertEquals(0, audit.invalidCount)
        assertEquals(428_298_258L, audit.savedBytes)
        assertEquals(92, audit.savedRatioPercent)
        assertEquals("Compressed copy verified", audit.title)
        assertEquals("1/1 output is public and smaller. Saved 428 MB (92%).", audit.summary)
        assertEquals(
            "Open the smaller copy from Movies/AI Photo Cleaner before deleting originals if you want to spot-check playback.",
            audit.guidance,
        )
    }

    @Test
    fun outputAuditBlocksTrustWhenAnyOutputFailsVerification() {
        val audit = LargeVideoCompressionOutputAuditState.fromResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "valid.mp4",
                    outputPath = "content://media/external/video/media/1000009002",
                    originalBytes = 900_000_000L,
                    outputBytes = 300_000_000L,
                ),
                LargeVideoCompressionResult(
                    sourceId = "private-output.mp4",
                    outputPath = "/sdcard/Android/data/com.air.cleaner/files/private.mp4",
                    originalBytes = 400_000_000L,
                    outputBytes = 120_000_000L,
                ),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertFalse(audit.canTrustSourceDelete)
        assertEquals(2, audit.checkedCount)
        assertEquals(1, audit.validCount)
        assertEquals(1, audit.invalidCount)
        assertEquals("Output check needs attention", audit.title)
        assertEquals("1/2 outputs passed. Fix 1 output before original deletion is trusted.", audit.summary)
        assertEquals(
            "Originals stay protected until every compressed copy is public, non-empty, and smaller than the source.",
            audit.guidance,
        )
    }

    @Test
    fun outputAuditExplainsMissingCompressionResults() {
        val audit = LargeVideoCompressionOutputAuditState.fromResults(
            results = emptyList(),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertFalse(audit.canTrustSourceDelete)
        assertEquals(0, audit.checkedCount)
        assertEquals("No compressed copies verified", audit.title)
        assertEquals("Compress at least one selected video before deleting originals.", audit.summary)
    }

    @Test
    fun compressedCopyOpenActionTargetsFirstVerifiedOutput() {
        val action = LargeVideoCompressedCopyOpenActionState.fromResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "private-output.mp4",
                    outputPath = "/sdcard/Android/data/com.air.cleaner/files/private.mp4",
                    originalBytes = 400_000_000L,
                    outputBytes = 120_000_000L,
                ),
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "content://media/external/video/media/1000009001",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                ),
                LargeVideoCompressionResult(
                    sourceId = "screen.mp4",
                    outputPath = "content://media/external/video/media/1000009002",
                    originalBytes = 1_000_000_000L,
                    outputBytes = 400_000_000L,
                ),
            ),
        )

        assertTrue(action.enabled)
        assertEquals("Open compressed copy", action.label)
        assertEquals("Spot-check first copy", action.title)
        assertEquals(
            "Opens 1 of 2 verified smaller copies before deleting originals.",
            action.subtitle,
        )
        assertEquals("content://media/external/video/media/1000009001", action.outputUri)
    }

    @Test
    fun compressedCopyOpenActionStaysDisabledWithoutVerifiedOutput() {
        val action = LargeVideoCompressedCopyOpenActionState.fromResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "not-smaller.mp4",
                    outputPath = "content://media/external/video/media/1000009003",
                    originalBytes = 400_000_000L,
                    outputBytes = 420_000_000L,
                ),
            ),
        )

        assertFalse(action.enabled)
        assertEquals("Open compressed copy", action.label)
        assertEquals("No playable copy ready", action.title)
        assertEquals(
            "Create a verified smaller public copy before spot-checking playback.",
            action.subtitle,
        )
        assertEquals(null, action.outputUri)
    }

    @Test
    fun postDeleteStatusConfirmsOriginalsRemovedAfterMediaStoreRecheck() {
        val summary = PhotoDeletionSummary(
            itemCount = 2,
            bytesToDelete = 3_000_000_000L,
            contentUris = listOf("content://video/travel.mov", "content://video/screen.mp4"),
        )

        val status = LargeVideoPostDeleteStatus.from(
            summary = summary,
            result = PhotoDeletionResult.fromSystemResult(summary, systemConfirmed = true),
            stillExistingContentUris = emptyList(),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals("Originals removed", status.title)
        assertEquals("Android confirmed deletion and MediaStore no longer lists the selected originals.", status.message)
        assertEquals("2 originals", status.requestedLabel)
        assertEquals("3000 MB", status.freedLabel)
        assertEquals("0 still present", status.remainingLabel)
        assertTrue(status.confirmedFreed)
    }

    @Test
    fun postDeleteStatusCallsOutOriginalsStillPresentAfterSystemConfirmation() {
        val summary = PhotoDeletionSummary(
            itemCount = 2,
            bytesToDelete = 3_000_000_000L,
            contentUris = listOf("content://video/travel.mov", "content://video/screen.mp4"),
        )

        val status = LargeVideoPostDeleteStatus.from(
            summary = summary,
            result = PhotoDeletionResult.fromSystemResult(summary, systemConfirmed = true),
            stillExistingContentUris = listOf("content://video/screen.mp4"),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals("1 original still present", status.title)
        assertEquals("Android returned success, but MediaStore still lists 1 selected original. Refresh or retry deletion before counting this space as freed.", status.message)
        assertEquals("1 still present", status.remainingLabel)
        assertFalse(status.confirmedFreed)
    }

    @Test
    fun postDeleteStatusExplainsCanceledSystemDelete() {
        val summary = PhotoDeletionSummary(
            itemCount = 1,
            bytesToDelete = 2_000_000_000L,
            contentUris = listOf("content://video/travel.mov"),
        )

        val status = LargeVideoPostDeleteStatus.from(
            summary = summary,
            result = PhotoDeletionResult.fromSystemResult(summary, systemConfirmed = false),
            stillExistingContentUris = null,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals("Originals kept", status.title)
        assertEquals("Android deletion was canceled. No original video was removed.", status.message)
        assertEquals("1 original", status.requestedLabel)
        assertEquals("0 MB", status.freedLabel)
        assertEquals("1 still present", status.remainingLabel)
        assertFalse(status.confirmedFreed)
    }

    @Test
    fun sampleLedgerAccumulatesCompressionCoverageAcrossRuns() {
        val firstRun = LargeVideoCompressionSampleLedger().appendCompressionResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "travel.mov",
                    outputPath = "content://media/external/video/media/42",
                    originalBytes = 2_000_000_000L,
                    outputBytes = 800_000_000L,
                    audioRemoved = true,
                ),
            ),
            profile = LargeVideoCompressionProfile.Balanced,
            sourceVideos = listOf(
                video("travel.mov", "Travel.mov", 2_000_000_000L, 185_000L, relativePath = "Pictures/AIPhotoCleanerRealSamples/huanqiu/"),
            ),
        )
        val secondRun = firstRun.appendCompressionResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "camera.mp4",
                    outputPath = "content://media/external/video/media/43",
                    originalBytes = 500_000_000L,
                    outputBytes = 220_000_000L,
                    audioRemoved = false,
                ),
            ),
            profile = LargeVideoCompressionProfile.HighQuality,
            sourceVideos = listOf(
                video("camera.mp4", "Camera.mp4", 500_000_000L, 45_000L, relativePath = "DCIM/Camera/"),
            ),
        )

        val matrix = secondRun.toSampleMatrix(requiredSampleCount = 2, requiredOriginCount = 2, requiredProfileCount = 2)

        assertEquals(2, secondRun.samples.size)
        assertEquals(2, matrix.sampleCount)
        assertEquals(2, matrix.originCount)
        assertEquals(2, matrix.profileCount)
        assertEquals(2, matrix.validOutputCount)
        assertEquals(1, matrix.videoOnlyCount)
        assertEquals(1, matrix.audioPreservedCount)
        assertEquals(100, matrix.readinessPercent)
        assertEquals(LargeVideoCompressionReleaseDecision.ReadyWithWarnings, matrix.decision)
        assertEquals(
            "Next: keep video-only copy explicit and add audio-compatible format targeting.",
            matrix.nextAction,
        )
    }

    @Test
    fun sampleLedgerKeepsLatestEvidenceWhenCapped() {
        val ledger = LargeVideoCompressionSampleLedger(
            samples = listOf(
                compressionSample(
                    sourceId = "old.mov",
                    sourceOrigin = "camera",
                    profile = LargeVideoCompressionProfile.StorageSaver,
                    originalBytes = 100_000_000L,
                    outputBytes = 40_000_000L,
                    audioRemoved = false,
                ),
            ),
        ).appendCompressionResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "new.mov",
                    outputPath = "content://media/external/video/media/44",
                    originalBytes = 300_000_000L,
                    outputBytes = 120_000_000L,
                    audioRemoved = false,
                ),
            ),
            profile = LargeVideoCompressionProfile.Balanced,
            sourceVideos = listOf(
                video("new.mov", "New.mov", 300_000_000L, 50_000L, relativePath = "Movies/Trips/"),
            ),
            maxSamples = 1,
        )

        assertEquals(listOf("new.mov"), ledger.samples.map { it.sourceId })
        assertEquals("movies", ledger.samples.single().sourceOrigin)
    }

    @Test
    fun sampleLedgerCapsByEvidenceTimeWhenLoadedOutOfOrder() {
        val ledger = LargeVideoCompressionSampleLedger(
            samples = listOf(
                compressionSample(
                    sourceId = "latest.mov",
                    sourceOrigin = "camera",
                    profile = LargeVideoCompressionProfile.Balanced,
                    originalBytes = 300_000_000L,
                    outputBytes = 120_000_000L,
                    audioRemoved = false,
                    capturedAtMillis = 300L,
                ),
                compressionSample(
                    sourceId = "oldest.mov",
                    sourceOrigin = "movies",
                    profile = LargeVideoCompressionProfile.StorageSaver,
                    originalBytes = 100_000_000L,
                    outputBytes = 40_000_000L,
                    audioRemoved = true,
                    capturedAtMillis = 100L,
                ),
            ),
        ).appendCompressionResults(
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "middle.mov",
                    outputPath = "content://media/external/video/media/45",
                    originalBytes = 200_000_000L,
                    outputBytes = 90_000_000L,
                    audioRemoved = false,
                ),
            ),
            profile = LargeVideoCompressionProfile.Balanced,
            sourceVideos = listOf(video("middle.mov", "Middle.mov", 200_000_000L, 60_000L)),
            maxSamples = 2,
            capturedAtMillis = 200L,
        )

        assertEquals(listOf("middle.mov", "latest.mov"), ledger.samples.map { it.sourceId })
    }

    @Test
    fun compressionAccessAllowsOneFreePreviewForNonPremiumUsers() {
        val access = LargeVideoCompressionAccessState.fromSelection(
            selectedCount = 1,
            isPremium = false,
            hasRewardedUnlock = false,
        )

        assertTrue(access.canStartCompression)
        assertFalse(access.requiresUnlock)
        assertEquals("Free preview", access.badgeLabel)
        assertEquals(
            "Compress 1 video free. Upgrade for unlimited large-video cleanup.",
            access.message,
        )
    }

    @Test
    fun compressionAccessLocksBatchCompressionForNonPremiumUsers() {
        val access = LargeVideoCompressionAccessState.fromSelection(
            selectedCount = 3,
            isPremium = false,
            hasRewardedUnlock = false,
        )

        assertFalse(access.canStartCompression)
        assertTrue(access.requiresUnlock)
        assertEquals("Premium", access.badgeLabel)
        assertEquals(
            "Batch compression is a premium cleanup. Select 1 video for a free preview or unlock unlimited compression.",
            access.message,
        )
    }

    @Test
    fun compressionAccessAllowsBatchCompressionForPremiumOrRewardedUnlock() {
        val premiumAccess = LargeVideoCompressionAccessState.fromSelection(
            selectedCount = 4,
            isPremium = true,
            hasRewardedUnlock = false,
        )
        val rewardedAccess = LargeVideoCompressionAccessState.fromSelection(
            selectedCount = 4,
            isPremium = false,
            hasRewardedUnlock = true,
        )

        assertTrue(premiumAccess.canStartCompression)
        assertFalse(premiumAccess.requiresUnlock)
        assertEquals("Unlimited", premiumAccess.badgeLabel)
        assertTrue(rewardedAccess.canStartCompression)
        assertFalse(rewardedAccess.requiresUnlock)
        assertEquals("Reward unlocked", rewardedAccess.badgeLabel)
    }

    private fun video(
        id: String,
        displayName: String,
        sizeBytes: Long,
        durationMillis: Long,
        relativePath: String = "Movies/Trips/",
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = 1_700_000_000_000L,
            contentHash = null,
            mediaType = MediaType.Video,
            contentUri = "content://video/$id",
            relativePath = relativePath,
            durationMillis = durationMillis,
        )
    }

    private fun compressionSample(
        sourceId: String,
        sourceOrigin: String,
        profile: LargeVideoCompressionProfile,
        originalBytes: Long,
        outputBytes: Long,
        audioRemoved: Boolean,
        capturedAtMillis: Long = 0L,
    ): LargeVideoCompressionSampleEvidence {
        return LargeVideoCompressionSampleEvidence(
            sourceId = sourceId,
            sourceOrigin = sourceOrigin,
            profile = profile,
            capturedAtMillis = capturedAtMillis,
            result = LargeVideoCompressionResult(
                sourceId = sourceId,
                outputPath = "content://media/external/video/media/$sourceId",
                originalBytes = originalBytes,
                outputBytes = outputBytes,
                audioRemoved = audioRemoved,
            ),
        )
    }
}
