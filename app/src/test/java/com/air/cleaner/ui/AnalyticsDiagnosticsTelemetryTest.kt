package com.air.cleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsDiagnosticsTelemetryTest {
    @Test
    fun productAnalyticsOptOutStillRecordsLocalDiagnostics() {
        val productEvents = mutableListOf<CleanerTelemetryEvent>()
        val diagnosticsEvents = mutableListOf<CleanerTelemetryEvent>()
        val telemetry = ProductAnalyticsWithDiagnosticsTelemetry(
            productTelemetry = RecordingCleanerTelemetry(productEvents),
            diagnosticsTelemetry = RecordingCleanerTelemetry(diagnosticsEvents),
            analyticsEnabled = { false },
        )
        val event = CleanerTelemetryEvent("similar_screenshots_entry_tapped", mapOf("groups_loaded" to false))

        telemetry.track(event)

        assertEquals(emptyList<CleanerTelemetryEvent>(), productEvents)
        assertEquals(listOf(event), diagnosticsEvents)
    }

    @Test
    fun productAnalyticsOptInRecordsProductAnalyticsAndLocalDiagnostics() {
        val productEvents = mutableListOf<CleanerTelemetryEvent>()
        val diagnosticsEvents = mutableListOf<CleanerTelemetryEvent>()
        val telemetry = ProductAnalyticsWithDiagnosticsTelemetry(
            productTelemetry = RecordingCleanerTelemetry(productEvents),
            diagnosticsTelemetry = RecordingCleanerTelemetry(diagnosticsEvents),
            analyticsEnabled = { true },
        )
        val event = CleanerTelemetryEvent("similar_screenshots_entry_tapped", mapOf("groups_loaded" to true))

        telemetry.track(event)

        assertEquals(listOf(event), productEvents)
        assertEquals(listOf(event), diagnosticsEvents)
    }

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
    fun defaultRecorderKeepsEnoughEventsForFullSimilarPhotosFunnel() {
        val snapshots = mutableListOf<List<CleanerTelemetryEvent>>()
        val telemetry = AnalyticsDiagnosticsTelemetry(
            onEventsChanged = { snapshots += it },
        )

        (1..8).forEach { index ->
            telemetry.track(CleanerTelemetryEvent("similar_step_$index", mapOf("index" to index)))
        }

        assertEquals(8, snapshots.last().size)
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
            Similar photos scan: no scan completed event yet.
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
            Similar photos scan: no scan completed event yet.
            Last local event: similar_screenshots_entry_tapped
            Recent events:
            1. similar_screenshots_entry_tapped | groups_loaded=false
            """.trimIndent(),
            shareContent.text,
        )
    }

    @Test
    fun diagnosticsReportExplainsEmptyResultWithScreenshotsAvailable() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 6L,
                    "group_count" to 0L,
                    "empty_result" to true,
                    "elapsed_ms" to 1_240L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "similar_screenshots_entry_tapped",
                properties = mapOf("groups_loaded" to false),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertEquals(
            """
            AI Photo Cleaner diagnostics
            Product analytics: disabled
            Similar photos funnel: 2/8
            Next: review the similar photo groups.
            Similar photos scan: screenshots=6, groups=0, empty=true, elapsed_ms=1240.
            Diagnosis: screenshots were scanned, but no similar groups matched. Next: test with 2-3 near-duplicate screenshots of the same screen or tune the matching threshold.
            Last local event: similar_screenshots_scan_completed
            Recent events:
            1. similar_screenshots_scan_completed | elapsed_ms=1240, empty_result=true, group_count=0, screenshot_count=6
            2. similar_screenshots_entry_tapped | groups_loaded=false
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun diagnosticsReportExplainsEmptyResultWithNoScreenshotsVisible() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 0L,
                    "group_count" to 0L,
                    "empty_result" to true,
                    "elapsed_ms" to 800L,
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = true)

        assertEquals(
            """
            AI Photo Cleaner diagnostics
            Product analytics: enabled
            Similar photos funnel: 1/8
            Next: review the similar photo groups.
            Similar photos scan: screenshots=0, groups=0, empty=true, elapsed_ms=800.
            Diagnosis: no screenshots were visible to the scan. Next: verify Photos permission and that screenshots are present in the media library.
            Last local event: similar_screenshots_scan_completed
            Recent events:
            1. similar_screenshots_scan_completed | elapsed_ms=800, empty_result=true, group_count=0, screenshot_count=0
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun diagnosticsReportExplainsFoundGroupsWithNoSelection() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_review_shown",
                properties = mapOf(
                    "group_count" to 1L,
                    "priority_groups" to 1L,
                    "recoverable_bytes" to 5_715_644L,
                    "selected_bytes" to 0L,
                    "selected_count" to 0L,
                    "status" to "fresh",
                ),
            ),
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 119L,
                    "group_count" to 1L,
                    "empty_result" to false,
                    "elapsed_ms" to 6_275L,
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertEquals(
            """
            AI Photo Cleaner diagnostics
            Product analytics: disabled
            Similar photos funnel: 2/8
            Next: adjust selection or tap Continue.
            Similar photos scan: screenshots=119, groups=1, empty=false, elapsed_ms=6275.
            Diagnosis: similar groups were found. Next: review selection quality and deletion confirmation.
            Similar photos review: groups=1, selected=0, selected_bytes=0.
            Diagnosis: similar groups were found, but no deletion candidates are selected. Next: tap Suggested or select candidates before Continue.
            Last local event: similar_screenshots_review_shown
            Recent events:
            1. similar_screenshots_review_shown | group_count=1, priority_groups=1, recoverable_bytes=5715644, selected_bytes=0, selected_count=0, status=fresh
            2. similar_screenshots_scan_completed | elapsed_ms=6275, empty_result=false, group_count=1, screenshot_count=119
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun diagnosticsReportSeparatesCacheHitFromFreshScanLatency() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 119L,
                    "group_count" to 1L,
                    "empty_result" to false,
                    "elapsed_ms" to 6_275L,
                    "scan_source" to "cold_scan",
                    "fingerprint_candidate_count" to 38L,
                    "fingerprint_skipped_count" to 81L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "similar_screenshots_cache_loaded",
                properties = mapOf(
                    "elapsed_ms" to 120L,
                    "cache_hit" to true,
                    "filtered_empty" to false,
                    "group_count" to 1L,
                    "recoverable_bytes" to 5_715_644L,
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertEquals(
            """
            AI Photo Cleaner diagnostics
            Product analytics: disabled
            Similar photos funnel: 1/8
            Next: review the similar photo groups.
            Similar photos cache: hit=true, groups=1, filtered_empty=false, elapsed_ms=120.
            Diagnosis: cache hit opened saved Similar photos results quickly while a fresh scan can continue in the background.
            Similar photos scan: source=cold_scan, screenshots=119, fingerprint_candidates=38, fingerprint_skipped=81, groups=1, empty=false, elapsed_ms=6275.
            Performance: cold scan is 2775ms over the 3500ms target. Next: reduce fingerprint candidates or reuse cached fingerprints before tuning match quality.
            Diagnosis: cold scan found similar groups after skipping 81 isolated screenshots before fingerprinting. Next: compare this latency against cache-hit time and optimize fingerprint reuse if cold scan stays above target.
            Last local event: similar_screenshots_scan_completed
            Recent events:
            1. similar_screenshots_scan_completed | elapsed_ms=6275, empty_result=false, fingerprint_candidate_count=38, fingerprint_skipped_count=81, group_count=1, scan_source=cold_scan, screenshot_count=119
            2. similar_screenshots_cache_loaded | cache_hit=true, elapsed_ms=120, filtered_empty=false, group_count=1, recoverable_bytes=5715644
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun diagnosticsReportFlagsColdScanAboveProductionLatencyTarget() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 119L,
                    "group_count" to 1L,
                    "empty_result" to false,
                    "elapsed_ms" to 6_275L,
                    "scan_source" to "cold_scan",
                    "fingerprint_candidate_count" to 38L,
                    "fingerprint_skipped_count" to 81L,
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertTrue(
            report.contains(
                "Performance: cold scan is 2775ms over the 3500ms target. Next: reduce fingerprint candidates or reuse cached fingerprints before tuning match quality.",
            ),
        )
    }

    @Test
    fun diagnosticsReportExplainsLowFingerprintCacheReuse() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 119L,
                    "group_count" to 1L,
                    "empty_result" to false,
                    "elapsed_ms" to 6_275L,
                    "scan_source" to "cold_scan",
                    "fingerprint_candidate_count" to 38L,
                    "fingerprint_skipped_count" to 81L,
                    "fingerprint_cache_hit_count" to 1L,
                    "fingerprint_cache_miss_count" to 37L,
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertTrue(
            report.contains(
                "Cache: fingerprint cache reused 1/38 candidates; 37 required decoding. Next: prioritize persistent fingerprint reuse before threshold tuning.",
            ),
        )
    }

    @Test
    fun diagnosticsReportExplainsHighFingerprintCacheReuseStillOverLatencyTarget() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 119L,
                    "group_count" to 1L,
                    "empty_result" to false,
                    "elapsed_ms" to 4_900L,
                    "scan_source" to "cold_scan",
                    "fingerprint_candidate_count" to 38L,
                    "fingerprint_skipped_count" to 81L,
                    "fingerprint_cache_hit_count" to 36L,
                    "fingerprint_cache_miss_count" to 2L,
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertTrue(
            report.contains(
                "Cache: fingerprint cache reused 36/38 candidates; 2 required decoding. Next: reduce fingerprint candidates or review matching work because cache reuse is already healthy but scan still exceeds target.",
            ),
        )
    }
}

private class RecordingCleanerTelemetry(
    private val events: MutableList<CleanerTelemetryEvent>,
) : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        events += event
    }
}
