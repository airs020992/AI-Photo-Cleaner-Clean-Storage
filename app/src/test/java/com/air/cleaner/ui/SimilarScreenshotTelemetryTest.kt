package com.air.cleaner.ui

import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import com.air.cleaner.feature.photos.PhotoDeletionSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarScreenshotTelemetryTest {
    @Test
    fun consentAwareTelemetryDropsEventsWhenAnalyticsIsDisabled() {
        val recordingTelemetry = RecordingCleanerTelemetry()
        val telemetry = ConsentAwareCleanerTelemetry(
            delegate = recordingTelemetry,
            analyticsEnabled = { false },
        )

        telemetry.track(CleanerTelemetryEvent(name = "test_event", properties = emptyMap()))

        assertEquals(emptyList<CleanerTelemetryEvent>(), recordingTelemetry.events)
    }

    @Test
    fun consentAwareTelemetryForwardsEventsWhenAnalyticsIsEnabled() {
        val recordingTelemetry = RecordingCleanerTelemetry()
        val telemetry = ConsentAwareCleanerTelemetry(
            delegate = recordingTelemetry,
            analyticsEnabled = { true },
        )
        val event = CleanerTelemetryEvent(name = "test_event", properties = mapOf("count" to 1))

        telemetry.track(event)

        assertEquals(listOf(event), recordingTelemetry.events)
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
    fun scanCompletedEventCapturesLatencyAndResultQuality() {
        val event = SimilarScreenshotTelemetry.scanCompleted(
            elapsedMillis = 1_240L,
            scanSummary = scanSummary(screenshotCount = 18),
            groups = listOf(group("safe", recoverableBytes = 2_000L)),
            status = SimilarScreenshotReviewStatus.Fresh,
        )

        assertEquals("similar_screenshots_scan_completed", event.name)
        assertEquals(
            mapOf<String, Any>(
                "elapsed_ms" to 1_240L,
                "screenshot_count" to 18,
                "group_count" to 1,
                "recoverable_bytes" to 2_000L,
                "status" to "fresh",
                "empty_result" to false,
            ),
            event.properties,
        )
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

    private fun scanSummary(screenshotCount: Int): MediaScanSummary {
        return MediaScanSummary(
            imageCount = screenshotCount,
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

    private class RecordingCleanerTelemetry : CleanerTelemetry {
        val events = mutableListOf<CleanerTelemetryEvent>()

        override fun track(event: CleanerTelemetryEvent) {
            events += event
        }
    }
}
