package com.air.cleaner.ui

import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.data.media.SimilarPhotoScanResult
import com.air.cleaner.data.media.SimilarScreenshotScanResult
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import com.air.cleaner.feature.photos.PhotoDeletionSummary
import com.air.cleaner.feature.photos.PhotoPostDeleteAction
import com.air.cleaner.feature.photos.PhotoPostDeleteMetric
import com.air.cleaner.feature.photos.PhotoPostDeleteStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarScreenshotTelemetryTest {
    @Test
    fun similarScreenshotEventsHaveStableGa4SafeContract() {
        val contracts = SimilarScreenshotAnalyticsContract.events

        assertEquals(
            setOf(
                "similar_photos_entry_tapped",
                "similar_photos_scan_completed",
                "similar_photos_review_shown",
                "similar_photos_selection_changed",
                "similar_photos_preview_action",
                "similar_photos_continue_tapped",
                "similar_photos_delete_requested",
                "similar_photos_system_delete_result",
                "similar_photos_post_delete_action",
                "large_videos_entry_tapped",
                "large_videos_scan_completed",
                "large_videos_review_shown",
                "large_videos_selection_changed",
                "large_videos_continue_tapped",
                "large_videos_delete_requested",
                "large_videos_system_delete_result",
                "large_videos_compression_completed",
                "large_videos_sample_matrix_updated",
                "similar_screenshots_entry_tapped",
                "similar_screenshots_cache_loaded",
                "similar_screenshots_rescan_tapped",
                "similar_screenshots_scan_completed",
                "similar_screenshots_review_shown",
                "similar_screenshots_selection_changed",
                "similar_screenshots_preview_action",
                "similar_screenshots_continue_tapped",
                "similar_screenshots_delete_requested",
                "similar_screenshots_system_delete_result",
                "similar_screenshots_post_delete_action",
            ),
            contracts.map { it.name }.toSet(),
        )
        contracts.forEach { contract ->
            assertEquals(true, contract.name.matches(Regex("[a-z][a-z0-9_]{0,39}")))
            contract.parameters.forEach { parameter ->
                assertEquals(true, parameter.name.matches(Regex("[a-z][a-z0-9_]{0,39}")))
                assertEquals(true, parameter.type in AnalyticsParameterType.entries)
            }
        }
    }

    @Test
    fun productAnalyticsWithDiagnosticsDropsProductEventsWhenAnalyticsIsDisabled() {
        val productTelemetry = RecordingCleanerTelemetry()
        val diagnosticsTelemetry = RecordingCleanerTelemetry()
        val telemetry = ProductAnalyticsWithDiagnosticsTelemetry(
            productTelemetry = productTelemetry,
            diagnosticsTelemetry = diagnosticsTelemetry,
            analyticsEnabled = { false },
        )
        val event = CleanerTelemetryEvent(name = "test_event", properties = emptyMap())

        telemetry.track(event)

        assertEquals(emptyList<CleanerTelemetryEvent>(), productTelemetry.events)
        assertEquals(listOf(event), diagnosticsTelemetry.events)
    }

    @Test
    fun productAnalyticsWithDiagnosticsForwardsProductEventsWhenAnalyticsIsEnabled() {
        val productTelemetry = RecordingCleanerTelemetry()
        val diagnosticsTelemetry = RecordingCleanerTelemetry()
        val telemetry = ProductAnalyticsWithDiagnosticsTelemetry(
            productTelemetry = productTelemetry,
            diagnosticsTelemetry = diagnosticsTelemetry,
            analyticsEnabled = { true },
        )
        val event = CleanerTelemetryEvent(name = "test_event", properties = mapOf("count" to 1))

        telemetry.track(event)

        assertEquals(listOf(event), productTelemetry.events)
        assertEquals(listOf(event), diagnosticsTelemetry.events)
    }

    @Test
    fun safeTelemetryDropsUnknownAndSensitiveFieldsBeforeForwarding() {
        val recordingTelemetry = RecordingCleanerTelemetry()
        val telemetry = SafeCleanerTelemetry(delegate = recordingTelemetry)
        val event = CleanerTelemetryEvent(
            name = "similar_screenshots_scan_completed",
            properties = mapOf(
                "elapsed_ms" to 800L,
                "status" to "fresh",
                "empty_result" to false,
                "content_uri" to "content://images/private",
                "display_name" to "bank-statement.png",
                "content_hash" to "abc123",
                "unexpected" to "drop-me",
            ),
        )

        telemetry.track(event)

        assertEquals(
            listOf(
                CleanerTelemetryEvent(
                    name = "similar_screenshots_scan_completed",
                    properties = mapOf<String, Any>(
                        "elapsed_ms" to 800L,
                        "status" to "fresh",
                        "empty_result" to false,
                    ),
                ),
            ),
            recordingTelemetry.events,
        )
    }

    @Test
    fun safeTelemetryDropsUnknownEventNames() {
        val recordingTelemetry = RecordingCleanerTelemetry()
        val telemetry = SafeCleanerTelemetry(delegate = recordingTelemetry)

        telemetry.track(
            CleanerTelemetryEvent(
                name = "similar_screenshots_private_debug",
                properties = mapOf("elapsed_ms" to 800L),
            ),
        )

        assertEquals(emptyList<CleanerTelemetryEvent>(), recordingTelemetry.events)
    }

    @Test
    fun safeTelemetryKeepsOnlyPropertiesAllowedForThatEvent() {
        val recordingTelemetry = RecordingCleanerTelemetry()
        val telemetry = SafeCleanerTelemetry(delegate = recordingTelemetry)

        telemetry.track(
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "elapsed_ms" to 800L,
                    "selected_count" to 12,
                    "confirmed" to true,
                ),
            ),
        )

        assertEquals(
            listOf(
                CleanerTelemetryEvent(
                    name = "similar_screenshots_scan_completed",
                    properties = mapOf<String, Any>("elapsed_ms" to 800L),
                ),
            ),
            recordingTelemetry.events,
        )
    }

    @Test
    fun safeTelemetryAllowsSimilarPhotoEventsWithoutPrivatePhotoIdentity() {
        val recordingTelemetry = RecordingCleanerTelemetry()
        val telemetry = SafeCleanerTelemetry(delegate = recordingTelemetry)

        telemetry.track(
            CleanerTelemetryEvent(
                name = "similar_photos_scan_completed",
                properties = mapOf(
                    "elapsed_ms" to 1_600L,
                    "image_count" to 280,
                    "group_count" to 3,
                    "content_uri" to "content://images/private",
                    "display_name" to "family.jpg",
                ),
            ),
        )

        assertEquals(
            listOf(
                CleanerTelemetryEvent(
                    name = "similar_photos_scan_completed",
                    properties = mapOf<String, Any>(
                        "elapsed_ms" to 1_600L,
                        "image_count" to 280,
                        "group_count" to 3,
                    ),
                ),
            ),
            recordingTelemetry.events,
        )
    }

    @Test
    fun compositeTelemetryForwardsToEveryDelegate() {
        val firstTelemetry = RecordingCleanerTelemetry()
        val secondTelemetry = RecordingCleanerTelemetry()
        val telemetry = CompositeCleanerTelemetry(firstTelemetry, secondTelemetry)
        val event = CleanerTelemetryEvent(
            name = "similar_screenshots_entry_tapped",
            properties = mapOf("group_count" to 2),
        )

        telemetry.track(event)

        assertEquals(listOf(event), firstTelemetry.events)
        assertEquals(listOf(event), secondTelemetry.events)
    }

    @Test
    fun cacheLoadedEventCapturesCacheHitLatencyAndQuality() {
        val event = SimilarScreenshotTelemetry.cacheLoaded(
            elapsedMillis = 120L,
            groups = listOf(group("cached", recoverableBytes = 2_500L)),
            filteredToEmpty = false,
        )

        assertEquals("similar_screenshots_cache_loaded", event.name)
        assertEquals(
            mapOf<String, Any>(
                "elapsed_ms" to 120L,
                "cache_hit" to true,
                "filtered_empty" to false,
                "group_count" to 1,
                "recoverable_bytes" to 2_500L,
            ),
            event.properties,
        )
    }

    @Test
    fun scanCompletedEventCapturesLatencyAndResultQuality() {
        val event = SimilarScreenshotTelemetry.scanCompleted(
            elapsedMillis = 1_240L,
            scanSummary = scanSummary(screenshotCount = 18),
            result = SimilarScreenshotScanResult(
                groups = listOf(group("safe", recoverableBytes = 2_000L)),
                fingerprintCandidateCount = 4,
                fingerprintSkippedCount = 14,
                fingerprintTimeSkippedCount = 8,
                fingerprintSizeSkippedCount = 6,
                fingerprintCacheHitCount = 3,
                fingerprintCacheMissCount = 1,
            ),
            status = SimilarScreenshotReviewStatus.Fresh,
            source = SimilarScreenshotScanSource.ColdScan,
        )

        assertEquals("similar_screenshots_scan_completed", event.name)
        assertEquals(
            mapOf<String, Any>(
                "elapsed_ms" to 1_240L,
                "screenshot_count" to 18,
                "fingerprint_candidate_count" to 4,
                "fingerprint_skipped_count" to 14,
                "fingerprint_time_skipped_count" to 8,
                "fingerprint_size_skipped_count" to 6,
                "fingerprint_cache_hit_count" to 3,
                "fingerprint_cache_miss_count" to 1,
                "group_count" to 1,
                "recoverable_bytes" to 2_000L,
                "status" to "fresh",
                "empty_result" to false,
                "scan_source" to "cold_scan",
            ),
            event.properties,
        )
    }

    @Test
    fun similarPhotoScanCompletedEventCapturesLatencyAndCandidateQuality() {
        val event = SimilarPhotoTelemetry.scanCompleted(
            elapsedMillis = 1_650L,
            scanSummary = scanSummary(imageCount = 280, screenshotCount = 18),
            result = SimilarPhotoScanResult(
                groups = listOf(group("similar-photo", recoverableBytes = 4_800L)),
                fingerprintCandidateCount = 42,
                fingerprintSkippedCount = 238,
                fingerprintTimeSkippedCount = 160,
                fingerprintSizeSkippedCount = 78,
                fingerprintCacheHitCount = 38,
                fingerprintCacheMissCount = 4,
            ),
        )

        assertEquals("similar_photos_scan_completed", event.name)
        assertEquals(
            mapOf<String, Any>(
                "elapsed_ms" to 1_650L,
                "image_count" to 280,
                "screenshot_count" to 18,
                "non_screenshot_count" to 262,
                "fingerprint_candidate_count" to 42,
                "fingerprint_skipped_count" to 238,
                "fingerprint_time_skipped_count" to 160,
                "fingerprint_size_skipped_count" to 78,
                "fingerprint_cache_hit_count" to 38,
                "fingerprint_cache_miss_count" to 4,
                "group_count" to 1,
                "recoverable_bytes" to 4_800L,
                "empty_result" to false,
            ),
            event.properties,
        )
    }

    @Test
    fun similarScreenshotRecoverableBytesUseNewestKeepStrategyForProductMetrics() {
        val group = similarScreenshotGroupWhereNewestIsNotLargest()

        val entryEvent = SimilarScreenshotTelemetry.entryTapped(
            groups = listOf(group),
            status = SimilarScreenshotReviewStatus.Fresh,
        )
        val scanEvent = SimilarScreenshotTelemetry.scanCompleted(
            elapsedMillis = 300L,
            scanSummary = scanSummary(screenshotCount = 3),
            result = SimilarScreenshotScanResult(
                groups = listOf(group),
                fingerprintCandidateCount = 3,
                fingerprintSkippedCount = 0,
            ),
            status = SimilarScreenshotReviewStatus.Fresh,
        )
        val reviewEvent = SimilarScreenshotTelemetry.reviewShown(
            groups = listOf(group),
            selectedCount = 2,
            selectedBytes = 7_000L,
            priorityGroups = 1,
            status = SimilarScreenshotReviewStatus.Fresh,
        )

        assertEquals(7_000L, entryEvent.properties["recoverable_bytes"])
        assertEquals(7_000L, scanEvent.properties["recoverable_bytes"])
        assertEquals(7_000L, reviewEvent.properties["recoverable_bytes"])
    }

    @Test
    fun continueEventCapturesSelectionPressureBeforeSystemDelete() {
        val event = SimilarScreenshotTelemetry.continueTapped(
            summary = PhotoDeletionSummary(
                itemCount = 3,
                bytesToDelete = 4_200L,
                contentUris = listOf("content://images/1", "content://images/2", "content://images/3"),
            ),
            totalGroups = 5,
            priorityGroups = 2,
        )

        assertEquals("similar_screenshots_continue_tapped", event.name)
        assertEquals(
            mapOf<String, Any>(
                "selected_count" to 3,
                "selected_bytes" to 4_200L,
                "total_groups" to 5,
                "priority_groups" to 2,
            ),
            event.properties,
        )
    }

    @Test
    fun reviewShownEventCapturesInitialReviewQuality() {
        val event = SimilarScreenshotTelemetry.reviewShown(
            groups = listOf(
                group("priority", recoverableBytes = 4_000L),
                group("normal", recoverableBytes = 2_000L),
            ),
            selectedCount = 3,
            selectedBytes = 6_000L,
            priorityGroups = 1,
            status = SimilarScreenshotReviewStatus.Fresh,
        )

        assertEquals("similar_screenshots_review_shown", event.name)
        assertEquals(
            mapOf<String, Any>(
                "group_count" to 2,
                "recoverable_bytes" to 6_000L,
                "selected_count" to 3,
                "selected_bytes" to 6_000L,
                "priority_groups" to 1,
                "status" to "fresh",
            ),
            event.properties,
        )
    }

    @Test
    fun selectionChangedEventCapturesUserTrustEditsWithoutPhotoIdentity() {
        val event = SimilarScreenshotTelemetry.selectionChanged(
            action = "group_clear",
            selectedCount = 1,
            selectedBytes = 2_500L,
            totalGroups = 4,
            priorityGroups = 2,
        )

        assertEquals("similar_screenshots_selection_changed", event.name)
        assertEquals(
            mapOf<String, Any>(
                "action" to "group_clear",
                "selected_count" to 1,
                "selected_bytes" to 2_500L,
                "total_groups" to 4,
                "priority_groups" to 2,
            ),
            event.properties,
        )
    }

    @Test
    fun previewActionEventCapturesComparisonBehaviorWithoutPhotoIdentity() {
        val event = SimilarScreenshotTelemetry.previewAction(
            action = "next",
            selectedCount = 2,
            selectedBytes = 3_500L,
            totalGroups = 4,
            priorityGroups = 1,
            photoIndex = 2,
            photoCount = 3,
            selectedForDeletion = true,
            recommendedKeep = false,
        )

        assertEquals("similar_screenshots_preview_action", event.name)
        assertEquals(
            mapOf<String, Any>(
                "action" to "next",
                "selected_count" to 2,
                "selected_bytes" to 3_500L,
                "total_groups" to 4,
                "priority_groups" to 1,
                "photo_index" to 2,
                "photo_count" to 3,
                "selected_for_deletion" to true,
                "recommended_keep" to false,
            ),
            event.properties,
        )
    }

    @Test
    fun deleteRequestedEventCapturesSystemDialogAvailability() {
        val event = SimilarScreenshotTelemetry.deleteRequested(
            summary = PhotoDeletionSummary(
                itemCount = 3,
                bytesToDelete = 4_200L,
                contentUris = listOf("content://images/1", "content://images/2"),
                highPriorityGroupCount = 1,
            ),
            systemDialogAvailable = false,
        )

        assertEquals("similar_screenshots_delete_requested", event.name)
        assertEquals(
            mapOf<String, Any>(
                "selected_count" to 3,
                "selected_bytes" to 4_200L,
                "priority_groups" to 1,
                "missing_access_count" to 1,
                "system_dialog_available" to false,
            ),
            event.properties,
        )
    }

    @Test
    fun postDeleteActionEventCapturesCleanupContinuationWithoutPhotoIdentity() {
        val event = SimilarScreenshotTelemetry.postDeleteAction(
            action = PhotoPostDeleteAction.ReviewPriorityGroups,
            status = PhotoPostDeleteStatus(
                title = "8 photos removed",
                message = "2 priority groups still need review",
                remainingGroupCount = 4,
                remainingRecoverableBytes = 4_000L,
                metrics = listOf(PhotoPostDeleteMetric("Priority remaining", "2 groups")),
                nextActionLabel = "Review priority groups next",
                nextAction = PhotoPostDeleteAction.ReviewPriorityGroups,
            ),
        )

        assertEquals("similar_screenshots_post_delete_action", event.name)
        assertEquals(
            mapOf<String, Any>(
                "action" to "review_priority_groups",
                "remaining_groups" to 4,
                "remaining_recoverable_bytes" to 4_000L,
                "has_priority_groups" to true,
            ),
            event.properties,
        )
    }

    private fun scanSummary(
        screenshotCount: Int,
        imageCount: Int = screenshotCount,
    ): MediaScanSummary {
        return MediaScanSummary(
            imageCount = imageCount,
            videoCount = 0,
            imageBytes = 0L,
            videoBytes = 0L,
            screenshotCount = screenshotCount,
            screenshotBytes = 0L,
        )
    }

    private fun group(
        key: String,
        recoverableBytes: Long,
    ): DuplicateGroup {
        return DuplicateGroup(
            key = key,
            items = listOf(
                MediaItem(
                    id = "$key-keep",
                    displayName = "$key-keep.jpg",
                    sizeBytes = 5_000L,
                    dateTakenMillis = 2_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-keep",
                ),
                MediaItem(
                    id = "$key-delete",
                    displayName = "$key-delete.jpg",
                    sizeBytes = recoverableBytes,
                    dateTakenMillis = 1_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-delete",
                ),
            ),
        )
    }

    private fun similarScreenshotGroupWhereNewestIsNotLargest(): DuplicateGroup {
        return DuplicateGroup(
            key = "similar",
            items = listOf(
                MediaItem(
                    id = "similar-newest",
                    displayName = "similar-newest.jpg",
                    sizeBytes = 1_000L,
                    dateTakenMillis = 3_000L,
                    contentHash = "similar",
                    mediaType = MediaType.Image,
                    contentUri = "content://images/similar-newest",
                ),
                MediaItem(
                    id = "similar-largest",
                    displayName = "similar-largest.jpg",
                    sizeBytes = 5_000L,
                    dateTakenMillis = 2_000L,
                    contentHash = "similar",
                    mediaType = MediaType.Image,
                    contentUri = "content://images/similar-largest",
                ),
                MediaItem(
                    id = "similar-middle",
                    displayName = "similar-middle.jpg",
                    sizeBytes = 2_000L,
                    dateTakenMillis = 1_000L,
                    contentHash = "similar",
                    mediaType = MediaType.Image,
                    contentUri = "content://images/similar-middle",
                ),
            ),
        )
    }

    private class RecordingCleanerTelemetry : CleanerTelemetry {
        val events = mutableListOf<CleanerTelemetryEvent>()

        override fun track(event: CleanerTelemetryEvent) {
            events += event
        }
    }
}
