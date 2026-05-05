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
    fun diagnosticsStatusLabelSeparatesLocalDiagnosticsFromProductAnalytics() {
        assertEquals(
            "Product analytics is off. Local diagnostics still records this session; Firebase and Logcat product events are paused.",
            analyticsDiagnosticsStatusLabel(
                analyticsEnabled = false,
                localEventCount = 3,
            ),
        )
        assertEquals(
            "Product analytics is enabled. Showing the last 3 local diagnostic events; Firebase and Logcat product events are also sent.",
            analyticsDiagnosticsStatusLabel(
                analyticsEnabled = true,
                localEventCount = 3,
            ),
        )
    }

    @Test
    fun deliveryDiagnosticsLabelsExplainFirebaseAndLogcatVerificationPath() {
        val labels = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_preview_action",
                properties = mapOf("action" to "open"),
            ),
        ).toAnalyticsDeliveryDiagnosticsLabels(
            analyticsEnabled = true,
            packageName = "com.air.cleaner",
        )

        assertEquals(
            listOf(
                "Firebase delivery: latest local event is eligible for Firebase and Logcat: similar_screenshots_preview_action.",
                "DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner",
                "Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots",
                "If Logcat shows FA-SVC Logging event but Firebase Console stays empty, check device network, proxy, SSL, and Google Play services rather than app instrumentation.",
            ),
            labels,
        )
    }

    @Test
    fun deliveryDiagnosticsLabelsExplainPausedOrIdleStates() {
        assertEquals(
            listOf(
                "Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events.",
                "DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner",
                "Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots",
            ),
            emptyList<CleanerTelemetryEvent>().toAnalyticsDeliveryDiagnosticsLabels(
                analyticsEnabled = false,
                packageName = "com.air.cleaner",
            ),
        )
        assertEquals(
            listOf(
                "Firebase delivery: ready. Trigger Photos > Similar photos to emit a test event.",
                "DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner",
                "Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots",
            ),
            emptyList<CleanerTelemetryEvent>().toAnalyticsDeliveryDiagnosticsLabels(
                analyticsEnabled = true,
                packageName = "com.air.cleaner",
            ),
        )
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
    fun defaultRecorderKeepsLargeVideoClosedLoopAlongsideBackgroundScans() {
        val snapshots = mutableListOf<List<CleanerTelemetryEvent>>()
        val telemetry = AnalyticsDiagnosticsTelemetry(
            onEventsChanged = { snapshots += it },
        )

        listOf(
            CleanerTelemetryEvent("similar_screenshots_cache_loaded", mapOf("elapsed_ms" to 32L)),
            CleanerTelemetryEvent("large_videos_scan_completed", mapOf("video_count" to 20L)),
            CleanerTelemetryEvent("similar_screenshots_scan_completed", mapOf("group_count" to 1L)),
            CleanerTelemetryEvent("similar_photos_scan_completed", mapOf("group_count" to 19L)),
            CleanerTelemetryEvent("large_videos_entry_tapped", mapOf("status" to "ready")),
            CleanerTelemetryEvent("large_videos_review_shown", mapOf("video_count" to 20L)),
            CleanerTelemetryEvent("large_videos_selection_changed", mapOf("action" to "clear")),
            CleanerTelemetryEvent("large_videos_selection_changed", mapOf("action" to "toggle")),
            CleanerTelemetryEvent("large_videos_continue_tapped", mapOf("selected_count" to 1L)),
            CleanerTelemetryEvent("large_videos_delete_requested", mapOf("selected_count" to 1L)),
            CleanerTelemetryEvent("large_videos_system_delete_result", mapOf("confirmed" to false)),
            CleanerTelemetryEvent("similar_screenshots_cache_loaded", mapOf("elapsed_ms" to 28L)),
            CleanerTelemetryEvent("similar_screenshots_scan_completed", mapOf("group_count" to 1L)),
            CleanerTelemetryEvent("similar_photos_scan_completed", mapOf("group_count" to 19L)),
        ).forEach(telemetry::track)

        val latestSnapshot = snapshots.last()
        assertTrue(latestSnapshot.any { it.name == "large_videos_entry_tapped" })
        assertTrue(latestSnapshot.any { it.name == "large_videos_system_delete_result" })
        assertEquals("Large videos funnel: 7/9", latestSnapshot.toAnalyticsDiagnosticsSummary().similarFunnelProgressLabel)
    }

    @Test
    fun safeTelemetryAllowsLargeVideoProductAnalyticsEvents() {
        val productEvents = mutableListOf<CleanerTelemetryEvent>()
        val telemetry = SafeCleanerTelemetry(RecordingCleanerTelemetry(productEvents))

        telemetry.track(
            CleanerTelemetryEvent(
                name = "large_videos_system_delete_result",
                properties = mapOf(
                    "confirmed" to false,
                    "selected_count" to 1L,
                    "selected_bytes" to 2_400_000_000L,
                    "unsafe_extra" to "drop",
                ),
            ),
        )

        assertEquals(1, productEvents.size)
        assertEquals("large_videos_system_delete_result", productEvents.single().name)
        assertEquals(
            mapOf(
                "confirmed" to false,
                "selected_count" to 1L,
                "selected_bytes" to 2_400_000_000L,
            ),
            productEvents.single().properties,
        )
    }

    @Test
    fun largeVideoDiagnosticsReportShowsCompressionSampleMatrixGate() {
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
        val events = listOf(
            LargeVideoTelemetry.compressionSampleMatrixUpdated(matrix),
            LargeVideoTelemetry.compressionCompleted(
                profile = LargeVideoCompressionProfile.Balanced,
                elapsedMillis = 190_000L,
                results = listOf(
                    LargeVideoCompressionResult(
                        sourceId = "DSCF6872.MOV",
                        outputPath = "content://media/external/video/media/1000008844",
                        originalBytes = 959_290_368L,
                        outputBytes = 36_117_812L,
                        audioRemoved = true,
                    ),
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertTrue(
            report.contains(
                "Large videos sample matrix: decision=blocked, readiness=20%, samples=1/5, origins=1/2, profiles=1/2, valid_outputs=1, video_only=1, audio_preserved=0, saved_ratio=96%.",
            ),
        )
        assertTrue(
            report.contains(
                "Diagnosis: sample matrix is blocked. Next: run at least 4 more real-device videos, including one audio-preserved output and another compression profile.",
            ),
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

        assertEquals("Similar photos funnel: 5/9", summary.similarFunnelProgressLabel)
        assertEquals("Next: confirm the Android delete dialog.", summary.similarFunnelNextStepLabel)
        assertEquals("Last local event: similar_screenshots_delete_requested", summary.latestEventLabel)
    }

    @Test
    fun diagnosticsSummaryExplainsNoSimilarPhotosEventsYet() {
        val summary = emptyList<CleanerTelemetryEvent>().toAnalyticsDiagnosticsSummary()

        assertEquals("Similar photos funnel: 0/9", summary.similarFunnelProgressLabel)
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
            Similar photos funnel: 2/9
            Next: closed loop captured.
            Similar photos scan: no scan completed event yet.
            Firebase delivery: latest local event is eligible for Firebase and Logcat: similar_screenshots_post_delete_action.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots
            If Logcat shows FA-SVC Logging event but Firebase Console stays empty, check device network, proxy, SSL, and Google Play services rather than app instrumentation.
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
            Similar photos funnel: 1/9
            Next: wait for the scan to finish.
            Similar photos scan: no scan completed event yet.
            Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots
            Last local event: similar_screenshots_entry_tapped
            Recent events:
            1. similar_screenshots_entry_tapped | groups_loaded=false
            """.trimIndent(),
            shareContent.text,
        )
    }

    @Test
    fun diagnosticsShareContentUsesProvidedPackageNameForDebugViewSetup() {
        val shareContent = emptyList<CleanerTelemetryEvent>().toAnalyticsDiagnosticsShareContent(
            analyticsEnabled = true,
            packageName = "com.example.cleaner",
        )

        assertTrue(
            shareContent.text.contains(
                "DebugView setup: adb shell setprop debug.firebase.analytics.app com.example.cleaner",
            ),
        )
    }

    @Test
    fun operationalDiagnosticsLabelsExposeCacheScanAndReviewSignalsForSettings() {
        val labels = listOf(
            CleanerTelemetryEvent(
                name = "similar_screenshots_review_shown",
                properties = mapOf(
                    "group_count" to 1L,
                    "selected_count" to 2L,
                    "selected_bytes" to 3_400_000L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "similar_screenshots_scan_completed",
                properties = mapOf(
                    "screenshot_count" to 121L,
                    "group_count" to 1L,
                    "empty_result" to false,
                    "elapsed_ms" to 4_200L,
                    "scan_source" to "cold_scan",
                    "fingerprint_candidate_count" to 52L,
                    "fingerprint_skipped_count" to 69L,
                    "fingerprint_cache_hit_count" to 52L,
                    "fingerprint_cache_miss_count" to 0L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "similar_screenshots_cache_loaded",
                properties = mapOf(
                    "elapsed_ms" to 73L,
                    "cache_hit" to true,
                    "filtered_empty" to false,
                    "group_count" to 1L,
                    "recoverable_bytes" to 5_715_644L,
                ),
            ),
        ).toAnalyticsOperationalDiagnosticsLabels()

        assertTrue(labels.any { it == "Similar photos cache: hit=true, groups=1, filtered_empty=false, elapsed_ms=73." })
        assertTrue(labels.any { it == "Performance: cold scan is 700ms over the 3500ms target. Next: reduce fingerprint candidates or reuse cached fingerprints before tuning match quality." })
        assertTrue(labels.any { it == "Similar photos review: groups=1, selected=2, selected_bytes=3400000." })
        assertTrue(labels.any { it == "Diagnosis: review is actionable. Next: tap Continue and confirm Android's delete dialog." })
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
            Similar photos funnel: 2/9
            Next: review the similar photo groups.
            Similar photos scan: screenshots=6, groups=0, empty=true, elapsed_ms=1240.
            Diagnosis: screenshots were scanned, but no similar groups matched. Next: test with 2-3 near-duplicate screenshots of the same screen or tune the matching threshold.
            Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots
            Last local event: similar_screenshots_scan_completed
            Recent events:
            1. similar_screenshots_scan_completed | elapsed_ms=1240, empty_result=true, group_count=0, screenshot_count=6
            2. similar_screenshots_entry_tapped | groups_loaded=false
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun diagnosticsReportExplainsSimilarPhotoScanCandidateQuality() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_photos_review_shown",
                properties = mapOf(
                    "group_count" to 3L,
                    "recoverable_bytes" to 33_000_000L,
                    "selected_count" to 24L,
                    "selected_bytes" to 33_000_000L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "similar_photos_scan_completed",
                properties = mapOf(
                    "elapsed_ms" to 1_650L,
                    "image_count" to 280L,
                    "screenshot_count" to 18L,
                    "non_screenshot_count" to 262L,
                    "fingerprint_candidate_count" to 42L,
                    "fingerprint_skipped_count" to 238L,
                    "fingerprint_time_skipped_count" to 160L,
                    "fingerprint_size_skipped_count" to 78L,
                    "fingerprint_cache_hit_count" to 38L,
                    "fingerprint_cache_miss_count" to 4L,
                    "group_count" to 3L,
                    "empty_result" to false,
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
            Similar photos scan: photos=262, screenshots_excluded=18, fingerprint_candidates=42, fingerprint_skipped=238, fingerprint_time_skipped=160, fingerprint_size_skipped=78, fingerprint_cache_hits=38, fingerprint_cache_misses=4, groups=3, empty=false, elapsed_ms=1650.
            Cache: similar photo fingerprint cache reused 38/42 candidates; 4 required decoding.
            Quality gate: candidate pruning passed; 42/262 photos fingerprinted (16%, target <=35%) and 238 skipped before decoding. Review load is 3 groups. Next: validate false positives with a labeled camera burst sample set.
            Diagnosis: similar photo groups were found. Next: review selection quality against real camera bursts and confirm deletion intent.
            Similar photos review: groups=3, selected=24, selected_bytes=33000000.
            Diagnosis: review is actionable. Next: tap Continue and confirm Android's delete dialog.
            Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_photos
            Last local event: similar_photos_review_shown
            Recent events:
            1. similar_photos_review_shown | group_count=3, recoverable_bytes=33000000, selected_bytes=33000000, selected_count=24
            2. similar_photos_scan_completed | elapsed_ms=1650, empty_result=false, fingerprint_cache_hit_count=38, fingerprint_cache_miss_count=4, fingerprint_candidate_count=42, fingerprint_size_skipped_count=78, fingerprint_skipped_count=238, fingerprint_time_skipped_count=160, group_count=3, image_count=280, non_screenshot_count=262, screenshot_count=18
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun largeVideoTelemetryCapturesScanAndSelectionMetrics() {
        val scanCompleted = LargeVideoTelemetry.scanCompleted(
            elapsedMillis = 420L,
            videos = listOf(
                largeVideo("travel.mov", 2_400_000_000L),
                largeVideo("screen.mp4", 720_000_000L),
            ),
        )
        val selectionChanged = LargeVideoTelemetry.selectionChanged(
            action = LargeVideoSelectionAction.Toggle,
            state = LargeVideoReviewSelectionState.fromVideos(
                listOf(
                    largeVideo("travel.mov", 2_400_000_000L),
                    largeVideo("screen.mp4", 720_000_000L),
                ),
            ).toggle("travel.mov"),
        )

        assertEquals("large_videos_scan_completed", scanCompleted.name)
        assertEquals(
            mapOf(
                "elapsed_ms" to 420L,
                "video_count" to 2,
                "recoverable_bytes" to 3_120_000_000L,
                "empty_result" to false,
            ),
            scanCompleted.properties,
        )
        assertEquals("large_videos_selection_changed", selectionChanged.name)
        assertEquals("toggle", selectionChanged.properties["action"])
        assertEquals(1, selectionChanged.properties["selected_count"])
        assertEquals(720_000_000L, selectionChanged.properties["selected_bytes"])
        assertEquals(2, selectionChanged.properties["video_count"])
    }

    @Test
    fun largeVideoTelemetryCapturesCompressionOutputQuality() {
        val event = LargeVideoTelemetry.compressionCompleted(
            profile = LargeVideoCompressionProfile.Balanced,
            elapsedMillis = 20_400L,
            results = listOf(
                LargeVideoCompressionResult(
                    sourceId = "DSCF6876.MOV",
                    outputPath = "content://media/external/video/media/8841",
                    originalBytes = 933_536_256L,
                    outputBytes = 35_111_080L,
                    audioRemoved = true,
                ),
            ),
        )

        assertEquals("large_videos_compression_completed", event.name)
        assertEquals(
            mapOf(
                "profile" to "balanced",
                "elapsed_ms" to 20_400L,
                "output_count" to 1,
                "original_bytes" to 933_536_256L,
                "output_bytes" to 35_111_080L,
                "saved_bytes" to 898_425_176L,
                "saved_ratio_percent" to 96L,
                "audio_removed_count" to 1,
                "public_output_count" to 1,
            ),
            event.properties,
        )
    }

    @Test
    fun largeVideoTelemetryCapturesCompressedCopyOpenResult() {
        val event = LargeVideoTelemetry.compressedCopyOpenRequested(
            result = LargeVideoCompressionResult(
                sourceId = "DSCF6876.MOV",
                outputPath = "content://media/external/video/media/8841",
                originalBytes = 933_536_256L,
                outputBytes = 35_111_080L,
                audioRemoved = true,
            ),
            opened = true,
        )

        assertEquals("large_videos_compressed_copy_open_requested", event.name)
        assertEquals(
            mapOf(
                "opened" to true,
                "public_output" to true,
                "source_id" to "DSCF6876.MOV",
                "original_bytes" to 933_536_256L,
                "output_bytes" to 35_111_080L,
                "saved_bytes" to 898_425_176L,
                "saved_ratio_percent" to 96L,
                "audio_removed" to true,
            ),
            event.properties,
        )
    }

    @Test
    fun largeVideoTelemetryUsesStableProfileNames() {
        fun profileName(profile: LargeVideoCompressionProfile): Any? {
            return LargeVideoTelemetry.compressionCompleted(
                profile = profile,
                elapsedMillis = 1L,
                results = emptyList(),
            ).properties["profile"]
        }

        assertEquals("storage_saver", profileName(LargeVideoCompressionProfile.StorageSaver))
        assertEquals("balanced", profileName(LargeVideoCompressionProfile.Balanced))
        assertEquals("high_quality", profileName(LargeVideoCompressionProfile.HighQuality))
    }

    @Test
    fun diagnosticsReportExplainsLargeVideoFunnelAndScanResult() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "large_videos_compression_completed",
                properties = mapOf(
                    "profile" to "balanced",
                    "elapsed_ms" to 20_400L,
                    "output_count" to 1L,
                    "original_bytes" to 933_536_256L,
                    "output_bytes" to 35_111_080L,
                    "saved_bytes" to 898_425_176L,
                    "saved_ratio_percent" to 96L,
                    "audio_removed_count" to 1L,
                    "public_output_count" to 1L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "large_videos_review_shown",
                properties = mapOf(
                    "video_count" to 20L,
                    "recoverable_bytes" to 13_200_000_000L,
                    "selected_count" to 20L,
                    "selected_bytes" to 13_200_000_000L,
                ),
            ),
            CleanerTelemetryEvent(
                name = "large_videos_scan_completed",
                properties = mapOf(
                    "elapsed_ms" to 420L,
                    "video_count" to 20L,
                    "recoverable_bytes" to 13_200_000_000L,
                    "empty_result" to false,
                ),
            ),
            CleanerTelemetryEvent(
                name = "large_videos_entry_tapped",
                properties = mapOf(
                    "video_count" to 20L,
                    "recoverable_bytes" to 13_200_000_000L,
                    "status" to "ready",
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertEquals(
            """
            AI Photo Cleaner diagnostics
            Product analytics: disabled
            Large videos funnel: 4/9
            Next: review output quality and delete originals only after Android confirmation.
            Large videos scan: videos=20, recoverable_bytes=13200000000, empty=false, elapsed_ms=420.
            Diagnosis: large videos were found. Next: review whether the largest originals are safe to delete.
            Large videos review: videos=20, selected=20, selected_bytes=13200000000.
            Diagnosis: review is actionable. Next: tap Continue and confirm Android's delete dialog.
            Large videos compression: outputs=1, original_bytes=933536256, output_bytes=35111080, saved_bytes=898425176, saved_ratio=96%, elapsed_ms=20400, profile=balanced, public_outputs=1, video_only=1.
            Diagnosis: compression created user-visible smaller copies. Next: sample playback quality, then delete originals only after Android confirmation.
            Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr large_videos
            Last local event: large_videos_compression_completed
            Recent events:
            1. large_videos_compression_completed | audio_removed_count=1, elapsed_ms=20400, original_bytes=933536256, output_bytes=35111080, output_count=1, profile=balanced, public_output_count=1, saved_bytes=898425176, saved_ratio_percent=96
            2. large_videos_review_shown | recoverable_bytes=13200000000, selected_bytes=13200000000, selected_count=20, video_count=20
            3. large_videos_scan_completed | elapsed_ms=420, empty_result=false, recoverable_bytes=13200000000, video_count=20
            4. large_videos_entry_tapped | recoverable_bytes=13200000000, status=ready, video_count=20
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun diagnosticsReportExplainsEmptyLargeVideoResult() {
        val report = listOf(
            CleanerTelemetryEvent(
                name = "large_videos_scan_completed",
                properties = mapOf(
                    "elapsed_ms" to 86L,
                    "video_count" to 0L,
                    "recoverable_bytes" to 0L,
                    "empty_result" to true,
                ),
            ),
        ).toAnalyticsDiagnosticsReport(analyticsEnabled = true)

        assertTrue(report.contains("Large videos funnel: 1/9"))
        assertTrue(
            report.contains(
                "Diagnosis: no large videos were visible to the scan. Next: verify Photos permission or import long screen recordings / camera videos.",
            ),
        )
    }

    @Test
    fun diagnosticsReportFlagsWeakSimilarPhotoCandidatePruning() {
        val events = listOf(
            CleanerTelemetryEvent(
                name = "similar_photos_scan_completed",
                properties = mapOf(
                    "elapsed_ms" to 2_400L,
                    "image_count" to 120L,
                    "screenshot_count" to 20L,
                    "non_screenshot_count" to 100L,
                    "fingerprint_candidate_count" to 62L,
                    "fingerprint_skipped_count" to 38L,
                    "group_count" to 1L,
                    "empty_result" to false,
                ),
            ),
        )

        val report = events.toAnalyticsDiagnosticsReport(analyticsEnabled = false)

        assertTrue(
            report.contains(
                "Quality gate: candidate pruning needs work; 62/100 photos were fingerprinted (62%, target <=35%) and only 38 skipped before decoding. Review load is 1 group. Next: tighten time and size prefilters before lowering the match threshold.",
            ),
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
            Similar photos funnel: 1/9
            Next: review the similar photo groups.
            Similar photos scan: screenshots=0, groups=0, empty=true, elapsed_ms=800.
            Diagnosis: no screenshots were visible to the scan. Next: verify Photos permission and that screenshots are present in the media library.
            Firebase delivery: latest local event is eligible for Firebase and Logcat: similar_screenshots_scan_completed.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots
            If Logcat shows FA-SVC Logging event but Firebase Console stays empty, check device network, proxy, SSL, and Google Play services rather than app instrumentation.
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
            Similar photos funnel: 2/9
            Next: adjust selection or tap Continue.
            Similar photos scan: screenshots=119, groups=1, empty=false, elapsed_ms=6275.
            Diagnosis: similar groups were found. Next: review selection quality and deletion confirmation.
            Similar photos review: groups=1, selected=0, selected_bytes=0.
            Diagnosis: similar groups were found, but no deletion candidates are selected. Next: tap Suggested or select candidates before Continue.
            Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots
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
                    "fingerprint_time_skipped_count" to 61L,
                    "fingerprint_size_skipped_count" to 20L,
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
            Similar photos funnel: 1/9
            Next: review the similar photo groups.
            Similar photos cache: hit=true, groups=1, filtered_empty=false, elapsed_ms=120.
            Diagnosis: cache hit opened saved Similar photos results quickly while a fresh scan can continue in the background.
            Similar photos scan: source=cold_scan, screenshots=119, fingerprint_candidates=38, fingerprint_skipped=81, fingerprint_time_skipped=61, fingerprint_size_skipped=20, groups=1, empty=false, elapsed_ms=6275.
            Performance: cold scan is 2775ms over the 3500ms target. Next: reduce fingerprint candidates or reuse cached fingerprints before tuning match quality.
            Diagnosis: cold scan found similar groups after skipping 81 isolated screenshots before fingerprinting. Next: compare this latency against cache-hit time and optimize fingerprint reuse if cold scan stays above target.
            Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events.
            DebugView setup: adb shell setprop debug.firebase.analytics.app com.air.cleaner
            Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr similar_screenshots
            Last local event: similar_screenshots_scan_completed
            Recent events:
            1. similar_screenshots_scan_completed | elapsed_ms=6275, empty_result=false, fingerprint_candidate_count=38, fingerprint_size_skipped_count=20, fingerprint_skipped_count=81, fingerprint_time_skipped_count=61, group_count=1, scan_source=cold_scan, screenshot_count=119
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

private fun largeVideo(id: String, sizeBytes: Long): com.air.cleaner.domain.cleaning.MediaItem {
    return com.air.cleaner.domain.cleaning.MediaItem(
        id = id,
        displayName = id,
        sizeBytes = sizeBytes,
        dateTakenMillis = 1_700_000_000_000L,
        contentHash = null,
        mediaType = com.air.cleaner.domain.cleaning.MediaType.Video,
        contentUri = "content://video/$id",
        durationMillis = 60_000L,
    )
}
