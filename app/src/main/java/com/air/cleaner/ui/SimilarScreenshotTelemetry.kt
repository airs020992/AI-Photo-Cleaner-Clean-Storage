package com.air.cleaner.ui

import android.util.Log
import android.os.Bundle
import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.data.media.SimilarPhotoScanResult
import com.air.cleaner.data.media.SimilarScreenshotScanResult
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.feature.photos.PhotoDeletionSummary
import com.air.cleaner.feature.photos.PhotoPostDeleteAction
import com.air.cleaner.feature.photos.PhotoPostDeleteStatus
import com.air.cleaner.feature.photos.PhotoReviewKeepStrategy
import com.air.cleaner.feature.photos.keepItem
import com.google.firebase.analytics.FirebaseAnalytics

internal data class CleanerTelemetryEvent(
    val name: String,
    val properties: Map<String, Any>,
)

internal data class AnalyticsDiagnosticsSummary(
    val latestEventLabel: String,
    val similarFunnelProgressLabel: String,
    val similarFunnelNextStepLabel: String,
    val similarScanInsightLabels: List<String>,
)

internal data class AnalyticsDiagnosticsShareContent(
    val title: String,
    val text: String,
    val unavailableMessage: String,
)

internal interface CleanerTelemetry {
    fun track(event: CleanerTelemetryEvent)
}

internal object NoOpCleanerTelemetry : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) = Unit
}

internal class ProductAnalyticsWithDiagnosticsTelemetry(
    private val productTelemetry: CleanerTelemetry,
    private val diagnosticsTelemetry: CleanerTelemetry,
    private val analyticsEnabled: () -> Boolean,
) : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        if (analyticsEnabled()) {
            productTelemetry.track(event)
        }
        diagnosticsTelemetry.track(event)
    }
}

internal class SafeCleanerTelemetry(
    private val delegate: CleanerTelemetry,
) : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        val safeProperties = SimilarScreenshotAnalyticsContract.safePropertyNames[event.name] ?: return
        delegate.track(
            event.copy(
                properties = event.properties.filterKeys { it in safeProperties },
            ),
        )
    }
}

internal enum class AnalyticsParameterType {
    Boolean,
    Long,
    String,
}

internal data class AnalyticsParameterContract(
    val name: String,
    val type: AnalyticsParameterType,
)

internal data class AnalyticsEventContract(
    val name: String,
    val parameters: List<AnalyticsParameterContract>,
)

internal object SimilarScreenshotAnalyticsContract {
    val events = listOf(
        AnalyticsEventContract(
            name = "similar_photos_entry_tapped",
            parameters = listOf(
                AnalyticsParameterContract("groups_loaded", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_scan_completed",
            parameters = listOf(
                AnalyticsParameterContract("elapsed_ms", AnalyticsParameterType.Long),
                AnalyticsParameterContract("empty_result", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("fingerprint_cache_hit_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_cache_miss_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_candidate_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_size_skipped_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_skipped_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_time_skipped_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("image_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("non_screenshot_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("screenshot_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_review_shown",
            parameters = listOf(
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_selection_changed",
            parameters = listOf(
                AnalyticsParameterContract("action", AnalyticsParameterType.String),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("total_groups", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_preview_action",
            parameters = listOf(
                AnalyticsParameterContract("action", AnalyticsParameterType.String),
                AnalyticsParameterContract("photo_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("photo_index", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recommended_keep", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_for_deletion", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("total_groups", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_continue_tapped",
            parameters = listOf(
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("total_groups", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_delete_requested",
            parameters = listOf(
                AnalyticsParameterContract("missing_access_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("system_dialog_available", AnalyticsParameterType.Boolean),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_system_delete_result",
            parameters = listOf(
                AnalyticsParameterContract("confirmed", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_photos_post_delete_action",
            parameters = listOf(
                AnalyticsParameterContract("action", AnalyticsParameterType.String),
                AnalyticsParameterContract("remaining_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("remaining_recoverable_bytes", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_entry_tapped",
            parameters = listOf(
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("status", AnalyticsParameterType.String),
                AnalyticsParameterContract("video_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_scan_completed",
            parameters = listOf(
                AnalyticsParameterContract("elapsed_ms", AnalyticsParameterType.Long),
                AnalyticsParameterContract("empty_result", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("video_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_review_shown",
            parameters = listOf(
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("video_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_selection_changed",
            parameters = listOf(
                AnalyticsParameterContract("action", AnalyticsParameterType.String),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("video_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_continue_tapped",
            parameters = listOf(
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("video_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_delete_requested",
            parameters = listOf(
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("system_dialog_available", AnalyticsParameterType.Boolean),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_system_delete_result",
            parameters = listOf(
                AnalyticsParameterContract("confirmed", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_compression_completed",
            parameters = listOf(
                AnalyticsParameterContract("audio_removed_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("elapsed_ms", AnalyticsParameterType.Long),
                AnalyticsParameterContract("original_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("output_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("output_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("profile", AnalyticsParameterType.String),
                AnalyticsParameterContract("public_output_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("saved_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("saved_ratio_percent", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "large_videos_sample_matrix_updated",
            parameters = listOf(
                AnalyticsParameterContract("audio_preserved_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("blocker_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("decision", AnalyticsParameterType.String),
                AnalyticsParameterContract("origin_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("output_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("profile_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("readiness_percent", AnalyticsParameterType.Long),
                AnalyticsParameterContract("required_origin_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("required_profile_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("required_sample_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("sample_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("saved_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("saved_ratio_percent", AnalyticsParameterType.Long),
                AnalyticsParameterContract("valid_output_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("video_only_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_entry_tapped",
            parameters = listOf(
                AnalyticsParameterContract("groups_loaded", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("status", AnalyticsParameterType.String),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_cache_loaded",
            parameters = listOf(
                AnalyticsParameterContract("cache_hit", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("elapsed_ms", AnalyticsParameterType.Long),
                AnalyticsParameterContract("filtered_empty", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_rescan_tapped",
            parameters = listOf(
                AnalyticsParameterContract("current_group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("status", AnalyticsParameterType.String),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_scan_completed",
            parameters = listOf(
                AnalyticsParameterContract("elapsed_ms", AnalyticsParameterType.Long),
                AnalyticsParameterContract("empty_result", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("fingerprint_cache_hit_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_cache_miss_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_candidate_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_size_skipped_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_skipped_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("fingerprint_time_skipped_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("scan_source", AnalyticsParameterType.String),
                AnalyticsParameterContract("screenshot_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("status", AnalyticsParameterType.String),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_review_shown",
            parameters = listOf(
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("priority_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("status", AnalyticsParameterType.String),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_selection_changed",
            parameters = listOf(
                AnalyticsParameterContract("action", AnalyticsParameterType.String),
                AnalyticsParameterContract("priority_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("total_groups", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_preview_action",
            parameters = listOf(
                AnalyticsParameterContract("action", AnalyticsParameterType.String),
                AnalyticsParameterContract("photo_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("photo_index", AnalyticsParameterType.Long),
                AnalyticsParameterContract("priority_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recommended_keep", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_for_deletion", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("total_groups", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_continue_tapped",
            parameters = listOf(
                AnalyticsParameterContract("priority_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("total_groups", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_delete_requested",
            parameters = listOf(
                AnalyticsParameterContract("missing_access_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("priority_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("system_dialog_available", AnalyticsParameterType.Boolean),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_system_delete_result",
            parameters = listOf(
                AnalyticsParameterContract("confirmed", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("priority_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_bytes", AnalyticsParameterType.Long),
                AnalyticsParameterContract("selected_count", AnalyticsParameterType.Long),
            ),
        ),
        AnalyticsEventContract(
            name = "similar_screenshots_post_delete_action",
            parameters = listOf(
                AnalyticsParameterContract("action", AnalyticsParameterType.String),
                AnalyticsParameterContract("has_priority_groups", AnalyticsParameterType.Boolean),
                AnalyticsParameterContract("remaining_groups", AnalyticsParameterType.Long),
                AnalyticsParameterContract("remaining_recoverable_bytes", AnalyticsParameterType.Long),
            ),
        ),
    )

    val safePropertyNames = events.associate { event ->
        event.name to event.parameters.map { parameter -> parameter.name }.toSet()
    }
}

internal class LogcatCleanerTelemetry : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        Log.d("AIPhotoCleaner", "${event.name} ${event.properties}")
    }
}

internal class FirebaseCleanerTelemetry(
    private val firebaseAnalytics: FirebaseAnalytics,
) : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        firebaseAnalytics.logEvent(event.name, event.properties.toFirebaseBundle())
    }
}

internal class CompositeCleanerTelemetry(
    private vararg val delegates: CleanerTelemetry,
) : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        delegates.forEach { delegate -> delegate.track(event) }
    }
}

internal class AnalyticsDiagnosticsTelemetry(
    private val maxEvents: Int = 24,
    private val onEventsChanged: (List<CleanerTelemetryEvent>) -> Unit,
) : CleanerTelemetry {
    private val recentEvents = ArrayDeque<CleanerTelemetryEvent>()

    override fun track(event: CleanerTelemetryEvent) {
        recentEvents.addFirst(event)
        while (recentEvents.size > maxEvents) {
            recentEvents.removeLast()
        }
        onEventsChanged(recentEvents.toList())
    }
}

internal fun List<CleanerTelemetryEvent>.toAnalyticsDiagnosticsSummary(): AnalyticsDiagnosticsSummary {
    val eventNames = map { it.name }.toSet()
    val hasLargeVideoEvents = eventNames.any { it.startsWith("large_videos_") }
    val hasSimilarPhotoEvents = eventNames.any { it.startsWith("similar_photos_") }
    val diagnosticsFunnel = when {
        hasLargeVideoEvents -> largeVideoDiagnosticsFunnel
        hasSimilarPhotoEvents -> similarPhotoDiagnosticsFunnel
        else -> similarScreenshotDiagnosticsFunnel
    }
    val funnelLabel = if (hasLargeVideoEvents) "Large videos" else "Similar photos"
    val completedSteps = diagnosticsFunnel.count { it.name in eventNames }
    val furthestStepIndex = diagnosticsFunnel.indexOfLast { it.name in eventNames }
    val nextActionLabel = if (furthestStepIndex == -1) {
        if (hasLargeVideoEvents) {
            "Next: open Videos > Large videos."
        } else {
            "Next: open Photos > Similar photos."
        }
    } else {
        diagnosticsFunnel[furthestStepIndex].nextActionLabel
    }
    return AnalyticsDiagnosticsSummary(
        latestEventLabel = firstOrNull()?.let { "Last local event: ${it.name}" } ?: "Last local event: none",
        similarFunnelProgressLabel = "$funnelLabel funnel: $completedSteps/${diagnosticsFunnel.size}",
        similarFunnelNextStepLabel = nextActionLabel,
        similarScanInsightLabels = when {
            hasLargeVideoEvents -> toLargeVideoScanInsightLabels()
            hasSimilarPhotoEvents -> toSimilarPhotoScanInsightLabels()
            else -> toSimilarScreenshotScanInsightLabels()
        },
    )
}

internal fun List<CleanerTelemetryEvent>.toAnalyticsDiagnosticsReport(
    analyticsEnabled: Boolean,
    packageName: String = "com.air.cleaner",
    mediaAccessDiagnosticsLabels: List<String> = emptyList(),
): String {
    val summary = toAnalyticsDiagnosticsSummary()
    val recentEventLines = if (isEmpty()) {
        listOf("none")
    } else {
        mapIndexed { index, event ->
            "${index + 1}. ${event.name} | ${event.properties.toStableDiagnosticsLabel()}"
        }
    }
    return buildList {
        add("AI Photo Cleaner diagnostics")
        add("Product analytics: ${if (analyticsEnabled) "enabled" else "disabled"}")
        addAll(mediaAccessDiagnosticsLabels)
        add(summary.similarFunnelProgressLabel)
        add(summary.similarFunnelNextStepLabel)
        addAll(toAnalyticsOperationalDiagnosticsLabels())
        addAll(toAnalyticsDeliveryDiagnosticsLabels(analyticsEnabled = analyticsEnabled, packageName = packageName))
        add(summary.latestEventLabel)
        add("Recent events:")
        addAll(recentEventLines)
    }.joinToString(separator = "\n")
}

internal fun List<CleanerTelemetryEvent>.toAnalyticsDiagnosticsShareContent(
    analyticsEnabled: Boolean,
    packageName: String = "com.air.cleaner",
    mediaAccessDiagnosticsLabels: List<String> = emptyList(),
): AnalyticsDiagnosticsShareContent {
    return AnalyticsDiagnosticsShareContent(
        title = "AI Photo Cleaner diagnostics",
        text = toAnalyticsDiagnosticsReport(
            analyticsEnabled = analyticsEnabled,
            packageName = packageName,
            mediaAccessDiagnosticsLabels = mediaAccessDiagnosticsLabels,
        ),
        unavailableMessage = "No share target found. Diagnostics copied instead.",
    )
}

internal fun analyticsDiagnosticsStatusLabel(
    analyticsEnabled: Boolean,
    localEventCount: Int,
): String {
    return if (analyticsEnabled) {
        "Product analytics is enabled. Showing the last $localEventCount local diagnostic events; Firebase and Logcat product events are also sent."
    } else {
        "Product analytics is off. Local diagnostics still records this session; Firebase and Logcat product events are paused."
    }
}

internal fun List<CleanerTelemetryEvent>.toAnalyticsOperationalDiagnosticsLabels(): List<String> {
    val hasLargeVideoEvents = any { it.name.startsWith("large_videos_") }
    val hasSimilarPhotoEvents = any { it.name.startsWith("similar_photos_") }
    return buildList {
        if (hasLargeVideoEvents) {
            addAll(toLargeVideoScanInsightLabels())
            addAll(toLargeVideoReviewInsightLabels())
            addAll(toLargeVideoCompressionInsightLabels())
            addAll(toLargeVideoCompressionSampleMatrixInsightLabels())
        } else if (hasSimilarPhotoEvents) {
            addAll(toSimilarPhotoScanInsightLabels())
            addAll(toSimilarPhotoReviewInsightLabels())
        } else {
            addAll(toSimilarScreenshotCacheInsightLabels())
            addAll(toSimilarScreenshotScanInsightLabels())
            addAll(toSimilarScreenshotReviewInsightLabels())
        }
    }
}

internal fun List<CleanerTelemetryEvent>.toAnalyticsDeliveryDiagnosticsLabels(
    analyticsEnabled: Boolean,
    packageName: String,
): List<String> {
    val latestEventName = firstOrNull()?.name
    val logcatFilter = when {
        latestEventName?.startsWith("large_videos_") == true -> "large_videos"
        latestEventName?.startsWith("similar_photos_") == true -> "similar_photos"
        else -> "similar_screenshots"
    }
    val deliveryLabel = when {
        !analyticsEnabled ->
            "Firebase delivery: paused until Product analytics is enabled. Local diagnostics still records events."
        latestEventName == null ->
            "Firebase delivery: ready. Trigger Photos > Similar photos to emit a test event."
        else ->
            "Firebase delivery: latest local event is eligible for Firebase and Logcat: $latestEventName."
    }
    return buildList {
        add(deliveryLabel)
        add("DebugView setup: adb shell setprop debug.firebase.analytics.app $packageName")
        add("Logcat check: adb logcat -s FA FA-SVC AIPhotoCleaner | findstr $logcatFilter")
        if (analyticsEnabled && latestEventName != null) {
            add(
                "If Logcat shows FA-SVC Logging event but Firebase Console stays empty, check device network, proxy, SSL, and Google Play services rather than app instrumentation.",
            )
        }
    }
}

private data class SimilarScreenshotDiagnosticsStep(
    val name: String,
    val nextActionLabel: String,
)

private val largeVideoDiagnosticsFunnel = listOf(
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_entry_tapped",
        nextActionLabel = "Next: wait for the scan to finish.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_scan_completed",
        nextActionLabel = "Next: review the largest videos.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_review_shown",
        nextActionLabel = "Next: adjust selection or tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_compression_completed",
        nextActionLabel = "Next: review output quality and delete originals only after Android confirmation.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_sample_matrix_updated",
        nextActionLabel = "Next: expand the real-device sample matrix.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_selection_changed",
        nextActionLabel = "Next: tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_continue_tapped",
        nextActionLabel = "Next: confirm the in-app delete dialog.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_delete_requested",
        nextActionLabel = "Next: confirm the Android delete dialog.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "large_videos_system_delete_result",
        nextActionLabel = "Next: closed loop captured.",
    ),
)

private val similarPhotoDiagnosticsFunnel = listOf(
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_entry_tapped",
        nextActionLabel = "Next: wait for the scan to finish.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_scan_completed",
        nextActionLabel = "Next: review the similar photo groups.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_review_shown",
        nextActionLabel = "Next: adjust selection or tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_preview_action",
        nextActionLabel = "Next: compare previews, adjust selection, or tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_selection_changed",
        nextActionLabel = "Next: tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_continue_tapped",
        nextActionLabel = "Next: confirm the in-app delete dialog.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_delete_requested",
        nextActionLabel = "Next: confirm the Android delete dialog.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_photos_system_delete_result",
        nextActionLabel = "Next: closed loop captured.",
    ),
)

private val similarScreenshotDiagnosticsFunnel = listOf(
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_entry_tapped",
        nextActionLabel = "Next: wait for the scan to finish.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_scan_completed",
        nextActionLabel = "Next: review the similar photo groups.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_review_shown",
        nextActionLabel = "Next: adjust selection or tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_preview_action",
        nextActionLabel = "Next: compare previews, adjust selection, or tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_selection_changed",
        nextActionLabel = "Next: tap Continue.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_continue_tapped",
        nextActionLabel = "Next: confirm the in-app delete dialog.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_delete_requested",
        nextActionLabel = "Next: confirm the Android delete dialog.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_system_delete_result",
        nextActionLabel = "Next: tap the result card CTA.",
    ),
    SimilarScreenshotDiagnosticsStep(
        name = "similar_screenshots_post_delete_action",
        nextActionLabel = "Next: closed loop captured.",
    ),
)

private fun Map<String, Any>.toStableDiagnosticsLabel(): String {
    return entries.sortedBy { it.key }.joinToString(separator = ", ") { (key, value) -> "$key=$value" }
}

private fun List<CleanerTelemetryEvent>.toSimilarScreenshotScanInsightLabels(): List<String> {
    val scanCompleted = firstOrNull { it.name == "similar_screenshots_scan_completed" }
        ?: return listOf("Similar photos scan: no scan completed event yet.")
    val source = scanCompleted.properties["scan_source"] as? String
    val screenshotCount = scanCompleted.properties["screenshot_count"].asLongOrZero()
    val groupCount = scanCompleted.properties["group_count"].asLongOrZero()
    val fingerprintCacheHitCount = scanCompleted.properties["fingerprint_cache_hit_count"].asLongOrZero()
    val fingerprintCacheMissCount = scanCompleted.properties["fingerprint_cache_miss_count"].asLongOrZero()
    val fingerprintCandidateCount = scanCompleted.properties["fingerprint_candidate_count"].asLongOrZero()
    val fingerprintSkippedCount = scanCompleted.properties["fingerprint_skipped_count"].asLongOrZero()
    val fingerprintTimeSkippedCount = scanCompleted.properties["fingerprint_time_skipped_count"].asLongOrZero()
    val fingerprintSizeSkippedCount = scanCompleted.properties["fingerprint_size_skipped_count"].asLongOrZero()
    val emptyResult = scanCompleted.properties["empty_result"] as? Boolean ?: false
    val elapsedMillis = scanCompleted.properties["elapsed_ms"].asLongOrZero()
    val diagnosis = when {
        emptyResult && screenshotCount == 0L ->
            "Diagnosis: no screenshots were visible to the scan. Next: verify Photos permission and that screenshots are present in the media library."
        emptyResult ->
            "Diagnosis: screenshots were scanned, but no similar groups matched. Next: test with 2-3 near-duplicate screenshots of the same screen or tune the matching threshold."
        source == SimilarScreenshotScanSource.ColdScan.analyticsValue && fingerprintSkippedCount > 0L ->
            "Diagnosis: cold scan found similar groups after skipping $fingerprintSkippedCount isolated screenshots before fingerprinting. Next: compare this latency against cache-hit time and optimize fingerprint reuse if cold scan stays above target."
        source == SimilarScreenshotScanSource.ColdScan.analyticsValue ->
            "Diagnosis: cold scan found similar groups. Next: compare this latency against cache-hit time and optimize fingerprint reuse if cold scan stays above target."
        else ->
            "Diagnosis: similar groups were found. Next: review selection quality and deletion confirmation."
    }
    val scanLabelPrefix = if (source == null) {
        "Similar photos scan:"
    } else {
        "Similar photos scan: source=$source,"
    }
    val scanMetrics = buildList {
        add("screenshots=$screenshotCount")
        if (fingerprintCandidateCount > 0L || fingerprintSkippedCount > 0L) {
            add("fingerprint_candidates=$fingerprintCandidateCount")
            add("fingerprint_skipped=$fingerprintSkippedCount")
            if (fingerprintTimeSkippedCount > 0L || fingerprintSizeSkippedCount > 0L) {
                add("fingerprint_time_skipped=$fingerprintTimeSkippedCount")
                add("fingerprint_size_skipped=$fingerprintSizeSkippedCount")
            }
        }
        if (fingerprintCacheHitCount > 0L || fingerprintCacheMissCount > 0L) {
            add("fingerprint_cache_hits=$fingerprintCacheHitCount")
            add("fingerprint_cache_misses=$fingerprintCacheMissCount")
        }
        add("groups=$groupCount")
        add("empty=$emptyResult")
        add("elapsed_ms=$elapsedMillis")
    }.joinToString(separator = ", ")
    return buildList {
        add(
            "$scanLabelPrefix $scanMetrics.",
        )
        addAll(
            similarScreenshotScanPerformanceLabels(
                source = source,
                elapsedMillis = elapsedMillis,
            ),
        )
        addAll(
            similarScreenshotFingerprintCacheLabels(
                cacheHitCount = fingerprintCacheHitCount,
                cacheMissCount = fingerprintCacheMissCount,
                source = source,
                elapsedMillis = elapsedMillis,
            ),
        )
        add(
            diagnosis,
        )
    }
}

private fun List<CleanerTelemetryEvent>.toSimilarPhotoScanInsightLabels(): List<String> {
    val scanCompleted = firstOrNull { it.name == "similar_photos_scan_completed" }
        ?: return emptyList()
    val imageCount = scanCompleted.properties["image_count"].asLongOrZero()
    val screenshotCount = scanCompleted.properties["screenshot_count"].asLongOrZero()
    val nonScreenshotCount = scanCompleted.properties["non_screenshot_count"].asLongOrZero()
        .takeIf { it > 0L }
        ?: (imageCount - screenshotCount).coerceAtLeast(0L)
    val groupCount = scanCompleted.properties["group_count"].asLongOrZero()
    val fingerprintCacheHitCount = scanCompleted.properties["fingerprint_cache_hit_count"].asLongOrZero()
    val fingerprintCacheMissCount = scanCompleted.properties["fingerprint_cache_miss_count"].asLongOrZero()
    val fingerprintCandidateCount = scanCompleted.properties["fingerprint_candidate_count"].asLongOrZero()
    val fingerprintSkippedCount = scanCompleted.properties["fingerprint_skipped_count"].asLongOrZero()
    val fingerprintTimeSkippedCount = scanCompleted.properties["fingerprint_time_skipped_count"].asLongOrZero()
    val fingerprintSizeSkippedCount = scanCompleted.properties["fingerprint_size_skipped_count"].asLongOrZero()
    val emptyResult = scanCompleted.properties["empty_result"] as? Boolean ?: false
    val elapsedMillis = scanCompleted.properties["elapsed_ms"].asLongOrZero()
    val diagnosis = when {
        emptyResult && nonScreenshotCount == 0L ->
            "Diagnosis: no non-screenshot photos were visible to the scan. Next: verify Photos permission and camera photos in the media library."
        emptyResult ->
            "Diagnosis: camera and album photos were scanned, but no safe similar groups matched. Next: test with real burst shots or tune thresholds with a labeled sample set."
        else ->
            "Diagnosis: similar photo groups were found. Next: review selection quality against real camera bursts and confirm deletion intent."
    }
    val scanMetrics = buildList {
        add("photos=$nonScreenshotCount")
        add("screenshots_excluded=$screenshotCount")
        add("fingerprint_candidates=$fingerprintCandidateCount")
        add("fingerprint_skipped=$fingerprintSkippedCount")
        if (fingerprintTimeSkippedCount > 0L || fingerprintSizeSkippedCount > 0L) {
            add("fingerprint_time_skipped=$fingerprintTimeSkippedCount")
            add("fingerprint_size_skipped=$fingerprintSizeSkippedCount")
        }
        if (fingerprintCacheHitCount > 0L || fingerprintCacheMissCount > 0L) {
            add("fingerprint_cache_hits=$fingerprintCacheHitCount")
            add("fingerprint_cache_misses=$fingerprintCacheMissCount")
        }
        add("groups=$groupCount")
        add("empty=$emptyResult")
        add("elapsed_ms=$elapsedMillis")
    }.joinToString(separator = ", ")
    return buildList {
        add("Similar photos scan: $scanMetrics.")
        val lookupCount = fingerprintCacheHitCount + fingerprintCacheMissCount
        if (lookupCount > 0L) {
            add(
                "Cache: similar photo fingerprint cache reused $fingerprintCacheHitCount/$lookupCount candidates; $fingerprintCacheMissCount required decoding.",
            )
        }
        addAll(
            similarPhotoCandidatePruningLabels(
                nonScreenshotCount = nonScreenshotCount,
                fingerprintCandidateCount = fingerprintCandidateCount,
                fingerprintSkippedCount = fingerprintSkippedCount,
                groupCount = groupCount,
            ),
        )
        add(diagnosis)
    }
}

private fun similarPhotoCandidatePruningLabels(
    nonScreenshotCount: Long,
    fingerprintCandidateCount: Long,
    fingerprintSkippedCount: Long,
    groupCount: Long,
): List<String> {
    if (nonScreenshotCount <= 0L) return emptyList()
    val passed = fingerprintCandidateCount * 100 <= nonScreenshotCount * SIMILAR_PHOTO_CANDIDATE_RATIO_TARGET_PERCENT
    val candidatePercent = ((fingerprintCandidateCount * 100) / nonScreenshotCount).coerceAtLeast(0L)
    val groupLabel = if (groupCount == 1L) "1 group" else "$groupCount groups"
    val label = if (passed) {
        "Quality gate: candidate pruning passed; $fingerprintCandidateCount/$nonScreenshotCount photos fingerprinted ($candidatePercent%, target <=${SIMILAR_PHOTO_CANDIDATE_RATIO_TARGET_PERCENT}%) and $fingerprintSkippedCount skipped before decoding. Review load is $groupLabel. Next: validate false positives with a labeled camera burst sample set."
    } else {
        "Quality gate: candidate pruning needs work; $fingerprintCandidateCount/$nonScreenshotCount photos were fingerprinted ($candidatePercent%, target <=${SIMILAR_PHOTO_CANDIDATE_RATIO_TARGET_PERCENT}%) and only $fingerprintSkippedCount skipped before decoding. Review load is $groupLabel. Next: tighten time and size prefilters before lowering the match threshold."
    }
    return listOf(label)
}

private fun similarScreenshotFingerprintCacheLabels(
    cacheHitCount: Long,
    cacheMissCount: Long,
    source: String?,
    elapsedMillis: Long,
): List<String> {
    val lookupCount = cacheHitCount + cacheMissCount
    if (lookupCount <= 0L) return emptyList()
    val nextStep = when {
        cacheMissCount > cacheHitCount ->
            "prioritize persistent fingerprint reuse before threshold tuning"
        source == SimilarScreenshotScanSource.ColdScan.analyticsValue &&
            elapsedMillis > SIMILAR_SCREENSHOT_COLD_SCAN_TARGET_MS ->
            "reduce fingerprint candidates or review matching work because cache reuse is already healthy but scan still exceeds target"
        else ->
            "focus on reducing fingerprint candidates and review quality"
    }
    return listOf(
        "Cache: fingerprint cache reused $cacheHitCount/$lookupCount candidates; $cacheMissCount required decoding. Next: $nextStep.",
    )
}

private fun similarScreenshotScanPerformanceLabels(
    source: String?,
    elapsedMillis: Long,
): List<String> {
    if (source != SimilarScreenshotScanSource.ColdScan.analyticsValue) return emptyList()
    if (elapsedMillis <= SIMILAR_SCREENSHOT_COLD_SCAN_TARGET_MS) return emptyList()
    val overBudgetMillis = elapsedMillis - SIMILAR_SCREENSHOT_COLD_SCAN_TARGET_MS
    return listOf(
        "Performance: cold scan is ${overBudgetMillis}ms over the ${SIMILAR_SCREENSHOT_COLD_SCAN_TARGET_MS}ms target. Next: reduce fingerprint candidates or reuse cached fingerprints before tuning match quality.",
    )
}

private fun List<CleanerTelemetryEvent>.toSimilarScreenshotCacheInsightLabels(): List<String> {
    val cacheLoaded = firstOrNull { it.name == "similar_screenshots_cache_loaded" }
        ?: return emptyList()
    val cacheHit = cacheLoaded.properties["cache_hit"] as? Boolean ?: false
    val filteredEmpty = cacheLoaded.properties["filtered_empty"] as? Boolean ?: false
    val groupCount = cacheLoaded.properties["group_count"].asLongOrZero()
    val elapsedMillis = cacheLoaded.properties["elapsed_ms"].asLongOrZero()
    val diagnosis = if (cacheHit) {
        "Diagnosis: cache hit opened saved Similar photos results quickly while a fresh scan can continue in the background."
    } else {
        "Diagnosis: no saved Similar photos result was available, so the user waits for a fresh scan."
    }
    return listOf(
        "Similar photos cache: hit=$cacheHit, groups=$groupCount, filtered_empty=$filteredEmpty, elapsed_ms=$elapsedMillis.",
        diagnosis,
    )
}

private fun List<CleanerTelemetryEvent>.toSimilarScreenshotReviewInsightLabels(): List<String> {
    val reviewShown = firstOrNull { it.name == "similar_screenshots_review_shown" }
        ?: return emptyList()
    val groupCount = reviewShown.properties["group_count"].asLongOrZero()
    val selectedCount = reviewShown.properties["selected_count"].asLongOrZero()
    val selectedBytes = reviewShown.properties["selected_bytes"].asLongOrZero()
    val previewAction = firstOrNull { it.name == "similar_screenshots_preview_action" }
    val diagnosis = when {
        groupCount > 0L && selectedCount == 0L ->
            "Diagnosis: similar groups were found, but no deletion candidates are selected. Next: tap Suggested or select candidates before Continue."
        groupCount > 0L ->
            "Diagnosis: review is actionable. Next: tap Continue and confirm Android's delete dialog."
        else ->
            "Diagnosis: review opened with no groups. Next: rescan or verify the scan result."
    }
    return listOf(
        "Similar photos review: groups=$groupCount, selected=$selectedCount, selected_bytes=$selectedBytes.",
    ) + previewAction.toSimilarScreenshotPreviewInsightLabel() + listOf(
        diagnosis,
    )
}

private fun List<CleanerTelemetryEvent>.toSimilarPhotoReviewInsightLabels(): List<String> {
    val reviewShown = firstOrNull { it.name == "similar_photos_review_shown" }
        ?: return emptyList()
    val groupCount = reviewShown.properties["group_count"].asLongOrZero()
    val selectedCount = reviewShown.properties["selected_count"].asLongOrZero()
    val selectedBytes = reviewShown.properties["selected_bytes"].asLongOrZero()
    val previewAction = firstOrNull { it.name == "similar_photos_preview_action" }
    val diagnosis = when {
        groupCount > 0L && selectedCount == 0L ->
            "Diagnosis: similar photo groups were found, but no deletion candidates are selected. Next: select candidates only after preview confidence is clear."
        groupCount > 0L ->
            "Diagnosis: review is actionable. Next: tap Continue and confirm Android's delete dialog."
        else ->
            "Diagnosis: review opened with no groups. Next: rescan or verify the scan result."
    }
    return listOf(
        "Similar photos review: groups=$groupCount, selected=$selectedCount, selected_bytes=$selectedBytes.",
    ) + previewAction.toSimilarPhotoPreviewInsightLabel() + listOf(diagnosis)
}

private fun CleanerTelemetryEvent?.toSimilarPhotoPreviewInsightLabel(): List<String> {
    if (this == null) return emptyList()
    val action = properties["action"] as? String ?: "unknown"
    val photoIndex = properties["photo_index"].asLongOrZero()
    val photoCount = properties["photo_count"].asLongOrZero()
    val selectedForDeletion = properties["selected_for_deletion"] as? Boolean ?: false
    val recommendedKeep = properties["recommended_keep"] as? Boolean ?: false
    return listOf(
        "Similar photos preview: action=$action, photo=$photoIndex/$photoCount, selected_for_deletion=$selectedForDeletion, recommended_keep=$recommendedKeep.",
    )
}

private fun CleanerTelemetryEvent?.toSimilarScreenshotPreviewInsightLabel(): List<String> {
    if (this == null) return emptyList()
    val action = properties["action"] as? String ?: "unknown"
    val photoIndex = properties["photo_index"].asLongOrZero()
    val photoCount = properties["photo_count"].asLongOrZero()
    val selectedForDeletion = properties["selected_for_deletion"] as? Boolean ?: false
    val recommendedKeep = properties["recommended_keep"] as? Boolean ?: false
    return listOf(
        "Similar photos preview: action=$action, photo=$photoIndex/$photoCount, selected_for_deletion=$selectedForDeletion, recommended_keep=$recommendedKeep.",
    )
}

private fun Any?.asLongOrZero(): Long {
    return when (this) {
        is Long -> this
        is Int -> toLong()
        is Short -> toLong()
        is Byte -> toLong()
        else -> 0L
    }
}

private fun List<CleanerTelemetryEvent>.toLargeVideoScanInsightLabels(): List<String> {
    val scanCompleted = firstOrNull { it.name == "large_videos_scan_completed" }
        ?: return emptyList()
    val videoCount = scanCompleted.properties["video_count"].asLongOrZero()
    val recoverableBytes = scanCompleted.properties["recoverable_bytes"].asLongOrZero()
    val emptyResult = scanCompleted.properties["empty_result"] as? Boolean ?: false
    val elapsedMillis = scanCompleted.properties["elapsed_ms"].asLongOrZero()
    val diagnosis = when {
        emptyResult || videoCount == 0L ->
            "Diagnosis: no large videos were visible to the scan. Next: verify Photos permission or import long screen recordings / camera videos."
        else ->
            "Diagnosis: large videos were found. Next: review whether the largest originals are safe to delete."
    }
    return listOf(
        "Large videos scan: videos=$videoCount, recoverable_bytes=$recoverableBytes, empty=$emptyResult, elapsed_ms=$elapsedMillis.",
        diagnosis,
    )
}

private fun List<CleanerTelemetryEvent>.toLargeVideoReviewInsightLabels(): List<String> {
    val reviewShown = firstOrNull { it.name == "large_videos_review_shown" }
        ?: return emptyList()
    val videoCount = reviewShown.properties["video_count"].asLongOrZero()
    val selectedCount = reviewShown.properties["selected_count"].asLongOrZero()
    val selectedBytes = reviewShown.properties["selected_bytes"].asLongOrZero()
    val diagnosis = when {
        videoCount == 0L ->
            "Diagnosis: review opened with no videos. Next: return to Videos and wait for scan completion."
        selectedCount == 0L ->
            "Diagnosis: large videos were found, but no deletion candidates are selected. Next: select one or more videos before Continue."
        else ->
            "Diagnosis: review is actionable. Next: tap Continue and confirm Android's delete dialog."
    }
    return listOf(
        "Large videos review: videos=$videoCount, selected=$selectedCount, selected_bytes=$selectedBytes.",
        diagnosis,
    )
}

private fun List<CleanerTelemetryEvent>.toLargeVideoCompressionInsightLabels(): List<String> {
    val compressionCompleted = firstOrNull { it.name == "large_videos_compression_completed" }
        ?: return emptyList()
    val outputCount = compressionCompleted.properties["output_count"].asLongOrZero()
    val originalBytes = compressionCompleted.properties["original_bytes"].asLongOrZero()
    val outputBytes = compressionCompleted.properties["output_bytes"].asLongOrZero()
    val savedBytes = compressionCompleted.properties["saved_bytes"].asLongOrZero()
    val savedRatioPercent = compressionCompleted.properties["saved_ratio_percent"].asLongOrZero()
    val elapsedMillis = compressionCompleted.properties["elapsed_ms"].asLongOrZero()
    val publicOutputCount = compressionCompleted.properties["public_output_count"].asLongOrZero()
    val audioRemovedCount = compressionCompleted.properties["audio_removed_count"].asLongOrZero()
    val profile = compressionCompleted.properties["profile"] as? String ?: "unknown"
    val diagnosis = when {
        outputCount == 0L ->
            "Diagnosis: compression finished without outputs. Next: retry with one known-good camera video."
        publicOutputCount < outputCount ->
            "Diagnosis: compression created outputs, but not all are user-visible. Next: verify MediaStore publishing."
        savedBytes <= 0L || savedRatioPercent <= 0L ->
            "Diagnosis: compression did not save space. Next: block delete guidance and retry with a lower profile."
        else ->
            "Diagnosis: compression created user-visible smaller copies. Next: sample playback quality, then delete originals only after Android confirmation."
    }
    return listOf(
        "Large videos compression: outputs=$outputCount, original_bytes=$originalBytes, output_bytes=$outputBytes, saved_bytes=$savedBytes, saved_ratio=$savedRatioPercent%, elapsed_ms=$elapsedMillis, profile=$profile, public_outputs=$publicOutputCount, video_only=$audioRemovedCount.",
        diagnosis,
    )
}

private fun List<CleanerTelemetryEvent>.toLargeVideoCompressionSampleMatrixInsightLabels(): List<String> {
    val matrixUpdated = firstOrNull { it.name == "large_videos_sample_matrix_updated" }
        ?: return emptyList()
    val decision = matrixUpdated.properties["decision"] as? String ?: "unknown"
    val readinessPercent = matrixUpdated.properties["readiness_percent"].asLongOrZero()
    val sampleCount = matrixUpdated.properties["sample_count"].asLongOrZero()
    val requiredSampleCount = matrixUpdated.properties["required_sample_count"].asLongOrZero()
    val originCount = matrixUpdated.properties["origin_count"].asLongOrZero()
    val requiredOriginCount = matrixUpdated.properties["required_origin_count"].asLongOrZero()
    val profileCount = matrixUpdated.properties["profile_count"].asLongOrZero()
    val requiredProfileCount = matrixUpdated.properties["required_profile_count"].asLongOrZero()
    val validOutputCount = matrixUpdated.properties["valid_output_count"].asLongOrZero()
    val videoOnlyCount = matrixUpdated.properties["video_only_count"].asLongOrZero()
    val audioPreservedCount = matrixUpdated.properties["audio_preserved_count"].asLongOrZero()
    val savedRatioPercent = matrixUpdated.properties["saved_ratio_percent"].asLongOrZero()
    val missingSamples = (requiredSampleCount - sampleCount).coerceAtLeast(0L)
    val diagnosis = when {
        decision == "blocked" && audioPreservedCount == 0L ->
            "Diagnosis: sample matrix is blocked. Next: run at least $missingSamples more real-device ${if (missingSamples == 1L) "video" else "videos"}, including one audio-preserved output and another compression profile."
        decision == "blocked" ->
            "Diagnosis: sample matrix is blocked. Next: expand real-device coverage before treating compression as release-ready."
        decision == "ready_with_warnings" ->
            "Diagnosis: sample matrix is covered, but video-only fallbacks still need explicit UX and format targeting."
        else ->
            "Diagnosis: sample matrix is ready. Next: run deletion confirmation and playback spot checks before release."
    }
    return listOf(
        "Large videos sample matrix: decision=$decision, readiness=$readinessPercent%, samples=$sampleCount/$requiredSampleCount, origins=$originCount/$requiredOriginCount, profiles=$profileCount/$requiredProfileCount, valid_outputs=$validOutputCount, video_only=$videoOnlyCount, audio_preserved=$audioPreservedCount, saved_ratio=$savedRatioPercent%.",
        diagnosis,
    )
}

private fun Map<String, Any>.toFirebaseBundle(): Bundle {
    return Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Double -> putDouble(key, value)
                is Float -> putDouble(key, value.toDouble())
                is Int -> putLong(key, value.toLong())
                is Long -> putLong(key, value)
                is String -> putString(key, value)
            }
        }
    }
}

internal object LargeVideoTelemetry {
    fun entryTapped(videos: List<MediaItem>?): CleanerTelemetryEvent {
        val loadedVideos = videos.orEmpty()
        return CleanerTelemetryEvent(
            name = "large_videos_entry_tapped",
            properties = mapOf(
                "status" to when {
                    videos == null -> "scanning"
                    loadedVideos.isEmpty() -> "empty"
                    else -> "ready"
                },
                "video_count" to loadedVideos.size,
                "recoverable_bytes" to loadedVideos.sumOf { video -> video.sizeBytes },
            ),
        )
    }

    fun scanCompleted(
        elapsedMillis: Long,
        videos: List<MediaItem>,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "large_videos_scan_completed",
            properties = mapOf(
                "elapsed_ms" to elapsedMillis.coerceAtLeast(0L),
                "video_count" to videos.size,
                "recoverable_bytes" to videos.sumOf { video -> video.sizeBytes },
                "empty_result" to videos.isEmpty(),
            ),
        )
    }

    fun reviewShown(state: LargeVideoReviewSelectionState): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "large_videos_review_shown",
            properties = state.toLargeVideoSelectionProperties(),
        )
    }

    fun selectionChanged(
        action: LargeVideoSelectionAction,
        state: LargeVideoReviewSelectionState,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "large_videos_selection_changed",
            properties = state.toLargeVideoSelectionProperties() + mapOf("action" to action.analyticsValue),
        )
    }

    fun continueTapped(state: LargeVideoReviewSelectionState): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "large_videos_continue_tapped",
            properties = state.toLargeVideoSelectionProperties(),
        )
    }

    fun deleteRequested(
        summary: PhotoDeletionSummary,
        systemDialogAvailable: Boolean,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "large_videos_delete_requested",
            properties = mapOf(
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
                "system_dialog_available" to systemDialogAvailable,
            ),
        )
    }

    fun systemDeleteResult(
        confirmed: Boolean,
        summary: PhotoDeletionSummary,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "large_videos_system_delete_result",
            properties = mapOf(
                "confirmed" to confirmed,
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
            ),
        )
    }

    fun compressionCompleted(
        profile: LargeVideoCompressionProfile,
        elapsedMillis: Long,
        results: List<LargeVideoCompressionResult>,
    ): CleanerTelemetryEvent {
        val originalBytes = results.sumOf { result -> result.originalBytes }
        val outputBytes = results.sumOf { result -> result.outputBytes }
        val savedBytes = (originalBytes - outputBytes).coerceAtLeast(0L)
        val savedRatioPercent = if (originalBytes > 0L) {
            (savedBytes * 100L / originalBytes).coerceIn(0L, 100L)
        } else {
            0L
        }
        return CleanerTelemetryEvent(
            name = "large_videos_compression_completed",
            properties = mapOf(
                "profile" to profile.analyticsValue,
                "elapsed_ms" to elapsedMillis.coerceAtLeast(0L),
                "output_count" to results.size,
                "original_bytes" to originalBytes,
                "output_bytes" to outputBytes,
                "saved_bytes" to savedBytes,
                "saved_ratio_percent" to savedRatioPercent,
                "audio_removed_count" to results.count { result -> result.audioRemoved },
                "public_output_count" to results.count { result ->
                    result.outputPath.startsWith("content://", ignoreCase = true)
                },
            ),
        )
    }

    fun compressedCopyOpenRequested(
        result: LargeVideoCompressionResult,
        opened: Boolean,
    ): CleanerTelemetryEvent {
        val savedBytes = (result.originalBytes - result.outputBytes).coerceAtLeast(0L)
        val savedRatioPercent = if (result.originalBytes > 0L) {
            (savedBytes * 100L / result.originalBytes).coerceIn(0L, 100L)
        } else {
            0L
        }
        return CleanerTelemetryEvent(
            name = "large_videos_compressed_copy_open_requested",
            properties = mapOf(
                "opened" to opened,
                "public_output" to result.outputPath.startsWith("content://", ignoreCase = true),
                "source_id" to result.sourceId,
                "original_bytes" to result.originalBytes,
                "output_bytes" to result.outputBytes,
                "saved_bytes" to savedBytes,
                "saved_ratio_percent" to savedRatioPercent,
                "audio_removed" to result.audioRemoved,
            ),
        )
    }

    fun compressionSampleMatrixUpdated(
        matrix: LargeVideoCompressionSampleMatrix,
        requiredSampleCount: Int = 5,
        requiredOriginCount: Int = 2,
        requiredProfileCount: Int = 2,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "large_videos_sample_matrix_updated",
            properties = mapOf(
                "decision" to matrix.decision.analyticsValue,
                "sample_count" to matrix.sampleCount,
                "required_sample_count" to requiredSampleCount,
                "origin_count" to matrix.originCount,
                "required_origin_count" to requiredOriginCount,
                "profile_count" to matrix.profileCount,
                "required_profile_count" to requiredProfileCount,
                "valid_output_count" to matrix.validOutputCount,
                "blocker_count" to matrix.blockerCount,
                "readiness_percent" to matrix.readinessPercent,
                "video_only_count" to matrix.videoOnlyCount,
                "audio_preserved_count" to matrix.audioPreservedCount,
                "output_bytes" to matrix.outputBytes,
                "saved_bytes" to matrix.savedBytes,
                "saved_ratio_percent" to matrix.savedRatioPercent,
            ),
        )
    }
}

private val LargeVideoCompressionProfile.analyticsValue: String
    get() = when (this) {
        LargeVideoCompressionProfile.StorageSaver -> "storage_saver"
        LargeVideoCompressionProfile.Balanced -> "balanced"
        LargeVideoCompressionProfile.HighQuality -> "high_quality"
    }

private val LargeVideoCompressionReleaseDecision.analyticsValue: String
    get() = when (this) {
        LargeVideoCompressionReleaseDecision.Ready -> "ready"
        LargeVideoCompressionReleaseDecision.ReadyWithWarnings -> "ready_with_warnings"
        LargeVideoCompressionReleaseDecision.Blocked -> "blocked"
    }

internal enum class LargeVideoSelectionAction {
    Toggle,
    SelectAll,
    Clear,
}

private val LargeVideoSelectionAction.analyticsValue: String
    get() = when (this) {
        LargeVideoSelectionAction.Toggle -> "toggle"
        LargeVideoSelectionAction.SelectAll -> "select_all"
        LargeVideoSelectionAction.Clear -> "clear"
    }

private fun LargeVideoReviewSelectionState.toLargeVideoSelectionProperties(): Map<String, Any> {
    return mapOf(
        "video_count" to videos.size,
        "recoverable_bytes" to videos.sumOf { video -> video.sizeBytes },
        "selected_count" to selectedCount,
        "selected_bytes" to selectedBytes,
    )
}

internal object SimilarPhotoTelemetry {
    fun entryTapped(groups: List<DuplicateGroup>?): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_entry_tapped",
            properties = mapOf(
                "groups_loaded" to (groups != null),
                "group_count" to (groups?.size ?: 0),
                "recoverable_bytes" to groups.orEmpty().similarPhotoRecoverableBytes(),
            ),
        )
    }

    fun scanCompleted(
        elapsedMillis: Long,
        scanSummary: MediaScanSummary?,
        result: SimilarPhotoScanResult,
    ): CleanerTelemetryEvent {
        val groups = result.groups
        val imageCount = scanSummary?.imageCount ?: 0
        val screenshotCount = scanSummary?.screenshotCount ?: 0
        return CleanerTelemetryEvent(
            name = "similar_photos_scan_completed",
            properties = mapOf(
                "elapsed_ms" to elapsedMillis.coerceAtLeast(0L),
                "image_count" to imageCount,
                "screenshot_count" to screenshotCount,
                "non_screenshot_count" to (imageCount - screenshotCount).coerceAtLeast(0),
                "fingerprint_cache_hit_count" to result.fingerprintCacheHitCount,
                "fingerprint_cache_miss_count" to result.fingerprintCacheMissCount,
                "fingerprint_candidate_count" to result.fingerprintCandidateCount,
                "fingerprint_skipped_count" to result.fingerprintSkippedCount,
                "fingerprint_time_skipped_count" to result.fingerprintTimeSkippedCount,
                "fingerprint_size_skipped_count" to result.fingerprintSizeSkippedCount,
                "group_count" to groups.size,
                "recoverable_bytes" to groups.similarPhotoRecoverableBytes(),
                "empty_result" to groups.isEmpty(),
            ),
        )
    }

    fun reviewShown(
        groups: List<DuplicateGroup>,
        selectedCount: Int,
        selectedBytes: Long,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_review_shown",
            properties = mapOf(
                "group_count" to groups.size,
                "recoverable_bytes" to groups.similarPhotoRecoverableBytes(),
                "selected_count" to selectedCount,
                "selected_bytes" to selectedBytes,
            ),
        )
    }

    fun selectionChanged(
        action: String,
        selectedCount: Int,
        selectedBytes: Long,
        totalGroups: Int,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_selection_changed",
            properties = mapOf(
                "action" to action,
                "selected_count" to selectedCount,
                "selected_bytes" to selectedBytes,
                "total_groups" to totalGroups,
            ),
        )
    }

    fun previewAction(
        action: String,
        selectedCount: Int,
        selectedBytes: Long,
        totalGroups: Int,
        photoIndex: Int,
        photoCount: Int,
        selectedForDeletion: Boolean,
        recommendedKeep: Boolean,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_preview_action",
            properties = mapOf(
                "action" to action,
                "selected_count" to selectedCount,
                "selected_bytes" to selectedBytes,
                "total_groups" to totalGroups,
                "photo_index" to photoIndex.coerceAtLeast(1),
                "photo_count" to photoCount.coerceAtLeast(1),
                "selected_for_deletion" to selectedForDeletion,
                "recommended_keep" to recommendedKeep,
            ),
        )
    }

    fun continueTapped(
        summary: PhotoDeletionSummary,
        totalGroups: Int,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_continue_tapped",
            properties = mapOf(
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
                "total_groups" to totalGroups,
            ),
        )
    }

    fun deleteRequested(
        summary: PhotoDeletionSummary,
        systemDialogAvailable: Boolean,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_delete_requested",
            properties = mapOf(
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
                "missing_access_count" to summary.missingDeleteAccessCount,
                "system_dialog_available" to systemDialogAvailable,
            ),
        )
    }

    fun systemDeleteResult(
        confirmed: Boolean,
        summary: PhotoDeletionSummary,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_system_delete_result",
            properties = mapOf(
                "confirmed" to confirmed,
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
            ),
        )
    }

    fun postDeleteAction(
        action: PhotoPostDeleteAction,
        status: PhotoPostDeleteStatus,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_photos_post_delete_action",
            properties = mapOf(
                "action" to action.analyticsValue,
                "remaining_groups" to status.remainingGroupCount,
                "remaining_recoverable_bytes" to status.remainingRecoverableBytes,
            ),
        )
    }
}

internal object SimilarScreenshotTelemetry {
    fun entryTapped(
        groups: List<DuplicateGroup>?,
        status: SimilarScreenshotReviewStatus,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_entry_tapped",
            properties = mapOf(
                "groups_loaded" to (groups != null),
                "group_count" to (groups?.size ?: 0),
                "recoverable_bytes" to groups.orEmpty().similarScreenshotRecoverableBytes(),
                "status" to status.analyticsValue,
            ),
        )
    }

    fun cacheLoaded(
        elapsedMillis: Long,
        groups: List<DuplicateGroup>,
        filteredToEmpty: Boolean,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_cache_loaded",
            properties = mapOf(
                "elapsed_ms" to elapsedMillis.coerceAtLeast(0L),
                "cache_hit" to groups.isNotEmpty(),
                "filtered_empty" to filteredToEmpty,
                "group_count" to groups.size,
                "recoverable_bytes" to groups.similarScreenshotRecoverableBytes(),
            ),
        )
    }

    fun rescanTapped(
        status: SimilarScreenshotReviewStatus,
        currentGroupCount: Int,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_rescan_tapped",
            properties = mapOf(
                "status" to status.analyticsValue,
                "current_group_count" to currentGroupCount,
            ),
        )
    }

    fun scanCompleted(
        elapsedMillis: Long,
        scanSummary: MediaScanSummary?,
        result: SimilarScreenshotScanResult,
        status: SimilarScreenshotReviewStatus,
        source: SimilarScreenshotScanSource = SimilarScreenshotScanSource.ColdScan,
    ): CleanerTelemetryEvent {
        val groups = result.groups
        return CleanerTelemetryEvent(
            name = "similar_screenshots_scan_completed",
            properties = mapOf(
                "elapsed_ms" to elapsedMillis.coerceAtLeast(0L),
                "screenshot_count" to (scanSummary?.screenshotCount ?: 0),
                "fingerprint_cache_hit_count" to result.fingerprintCacheHitCount,
                "fingerprint_cache_miss_count" to result.fingerprintCacheMissCount,
                "fingerprint_candidate_count" to result.fingerprintCandidateCount,
                "fingerprint_skipped_count" to result.fingerprintSkippedCount,
                "fingerprint_time_skipped_count" to result.fingerprintTimeSkippedCount,
                "fingerprint_size_skipped_count" to result.fingerprintSizeSkippedCount,
                "group_count" to groups.size,
                "recoverable_bytes" to groups.similarScreenshotRecoverableBytes(),
                "status" to status.analyticsValue,
                "scan_source" to source.analyticsValue,
                "empty_result" to groups.isEmpty(),
            ),
        )
    }

    fun reviewShown(
        groups: List<DuplicateGroup>,
        selectedCount: Int,
        selectedBytes: Long,
        priorityGroups: Int,
        status: SimilarScreenshotReviewStatus,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_review_shown",
            properties = mapOf(
                "group_count" to groups.size,
                "recoverable_bytes" to groups.similarScreenshotRecoverableBytes(),
                "selected_count" to selectedCount,
                "selected_bytes" to selectedBytes,
                "priority_groups" to priorityGroups,
                "status" to status.analyticsValue,
            ),
        )
    }

    fun selectionChanged(
        action: String,
        selectedCount: Int,
        selectedBytes: Long,
        totalGroups: Int,
        priorityGroups: Int,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_selection_changed",
            properties = mapOf(
                "action" to action,
                "selected_count" to selectedCount,
                "selected_bytes" to selectedBytes,
                "total_groups" to totalGroups,
                "priority_groups" to priorityGroups,
            ),
        )
    }

    fun previewAction(
        action: String,
        selectedCount: Int,
        selectedBytes: Long,
        totalGroups: Int,
        priorityGroups: Int,
        photoIndex: Int,
        photoCount: Int,
        selectedForDeletion: Boolean,
        recommendedKeep: Boolean,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_preview_action",
            properties = mapOf(
                "action" to action,
                "selected_count" to selectedCount,
                "selected_bytes" to selectedBytes,
                "total_groups" to totalGroups,
                "priority_groups" to priorityGroups,
                "photo_index" to photoIndex.coerceAtLeast(1),
                "photo_count" to photoCount.coerceAtLeast(1),
                "selected_for_deletion" to selectedForDeletion,
                "recommended_keep" to recommendedKeep,
            ),
        )
    }

    fun continueTapped(
        summary: PhotoDeletionSummary,
        totalGroups: Int,
        priorityGroups: Int,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_continue_tapped",
            properties = mapOf(
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
                "total_groups" to totalGroups,
                "priority_groups" to priorityGroups,
            ),
        )
    }

    fun deleteRequested(
        summary: PhotoDeletionSummary,
        systemDialogAvailable: Boolean,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_delete_requested",
            properties = mapOf(
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
                "priority_groups" to summary.priorityGroupCount,
                "missing_access_count" to summary.missingDeleteAccessCount,
                "system_dialog_available" to systemDialogAvailable,
            ),
        )
    }

    fun systemDeleteResult(
        confirmed: Boolean,
        summary: PhotoDeletionSummary,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_system_delete_result",
            properties = mapOf(
                "confirmed" to confirmed,
                "selected_count" to summary.itemCount,
                "selected_bytes" to summary.bytesToDelete,
                "priority_groups" to summary.priorityGroupCount,
            ),
        )
    }

    fun postDeleteAction(
        action: PhotoPostDeleteAction,
        status: PhotoPostDeleteStatus,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_post_delete_action",
            properties = mapOf(
                "action" to action.analyticsValue,
                "remaining_groups" to status.remainingGroupCount,
                "remaining_recoverable_bytes" to status.remainingRecoverableBytes,
                "has_priority_groups" to (status.nextAction == PhotoPostDeleteAction.ReviewPriorityGroups),
            ),
        )
    }
}

internal enum class SimilarScreenshotScanSource {
    ColdScan,
    ManualRescan,
    PostDeleteRefresh,
}

internal fun List<DuplicateGroup>.similarScreenshotRecoverableBytes(): Long {
    return sumOf { group ->
        val keepId = group.keepItem(PhotoReviewKeepStrategy.Newest).id
        group.items
            .filterNot { item -> item.id == keepId }
            .sumOf { item -> item.sizeBytes }
    }
}

internal fun List<DuplicateGroup>.similarPhotoRecoverableBytes(): Long {
    return sumOf { it.recoverableBytes }
}

private const val SIMILAR_SCREENSHOT_COLD_SCAN_TARGET_MS = 3_500L
private const val SIMILAR_PHOTO_CANDIDATE_RATIO_TARGET_PERCENT = 35L

private val SimilarScreenshotScanSource.analyticsValue: String
    get() = when (this) {
        SimilarScreenshotScanSource.ColdScan -> "cold_scan"
        SimilarScreenshotScanSource.ManualRescan -> "manual_rescan"
        SimilarScreenshotScanSource.PostDeleteRefresh -> "post_delete_refresh"
    }

private val SimilarScreenshotReviewStatus.analyticsValue: String
    get() = name.lowercase()

private val PhotoPostDeleteAction.analyticsValue: String
    get() = when (this) {
        PhotoPostDeleteAction.ReturnToPhotos -> "return_to_photos"
        PhotoPostDeleteAction.ReviewRemainingGroups -> "review_remaining_groups"
        PhotoPostDeleteAction.ReviewPriorityGroups -> "review_priority_groups"
    }
