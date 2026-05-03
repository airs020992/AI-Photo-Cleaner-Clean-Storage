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
}
