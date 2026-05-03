package com.air.cleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsDiagnosticsTelemetryTest {
    @Test
    fun recorderKeepsMostRecentEventsFirstWithinLimit() {
        val snapshots = mutableListOf<List<CleanerTelemetryEvent>>()
        val telemetry = AnalyticsDiagnosticsTelemetry(
            maxEvents = 2,
            onEventsChanged = { snapshots += it },
        )

        telemetry.track(CleanerTelemetryEvent("first", mapOf("count" to 1L)))
        telemetry.track(CleanerTelemetryEvent("second", mapOf("count" to 2L)))
        telemetry.track(CleanerTelemetryEvent("third", mapOf("count" to 3L)))

        assertEquals(
            listOf("third", "second"),
            snapshots.last().map { it.name },
        )
    }

    @Test
    fun diagnosticsSummaryShowsSimilarPhotosFunnelProgressAndNextAction() {
        val events = listOf(
            CleanerTelemetryEvent("similar_screenshots_delete_requested", mapOf("selected_count" to 3L)),
            CleanerTelemetryEvent("similar_screenshots_continue_tapped", mapOf("selected_count" to 3L)),
            CleanerTelemetryEvent("similar_screenshots_review_shown", mapOf("group_count" to 2L)),
            CleanerTelemetryEvent("similar_screenshots_scan_completed", mapOf("empty_result" to false)),
            CleanerTelemetryEvent("similar_screenshots_entry_tapped", mapOf("groups_loaded" to false)),
        )

        val summary = events.toAnalyticsDiagnosticsSummary()

        assertEquals("Similar photos funnel: 5/8", summary.similarFunnelProgressLabel)
        assertEquals("Next: confirm the Android delete dialog.", summary.similarFunnelNextStepLabel)
        assertEquals("Last local event: similar_screenshots_delete_requested", summary.latestEventLabel)
    }

    @Test
    fun diagnosticsSummaryExplainsNoSimilarPhotosEventsYet() {
        val summary = emptyList<CleanerTelemetryEvent>().toAnalyticsDiagnosticsSummary()

        assertEquals("Similar photos funnel: 0/8", summary.similarFunnelProgressLabel)
        assertEquals("Next: open Photos > Similar photos.", summary.similarFunnelNextStepLabel)
        assertEquals("Last local event: none", summary.latestEventLabel)
    }

    @Test
    fun diagnosticsReportIsCopyReadyForDeviceTriage() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_post_delete_action",
                properties = mapOf(
                    "action" to "review_priority_groups",
                    "remaining_groups" to 4L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "similar_screenshots_system_delete_result",
                properties = mapOf("confirmed" to true),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = true)

        assertEquals(
            """
            AI Photo Cleaner diagnostics
            Product analytics: enabled
            Similar photos funnel: 2/8
            Next: closed loop captured.
            Last local event: similar_screenshots_post_delete_action
            Recent events:
            1. similar_screenshots_post_delete_action | action=review_priority_groups, remaining_groups=4
            2. similar_screenshots_system_delete_result | confirmed=true
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun diagnosticsShareContentUsesStableTitleAndReportBody() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_entry_tapped",
                properties = mapOf("groups_loaded" to false),
            ),
        )

        val shareContent = events.toAnalyticsDiagnosticsShareContent(analyticsEnabled = false)

        assertEquals("AI Photo Cleaner diagnostics", shareContent.title)
        assertEquals("No share target found. Diagnostics copied instead.", shareContent.unavailableMessage)
        assertEquals(
            """
            AI Photo Cleaner diagnostics
            Product analytics: disabled
            Similar photos funnel: 1/8
            Next: wait for the scan to finish.
            Last local event: similar_screenshots_entry_tapped
            Recent events:
            1. similar_screenshots_entry_tapped | groups_loaded=false
            """.trimIndent(),
            shareContent.text,
        )
    }
}
