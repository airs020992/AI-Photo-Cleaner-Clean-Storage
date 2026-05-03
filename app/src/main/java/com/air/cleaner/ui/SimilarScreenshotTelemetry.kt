package com.air.cleaner.ui

import android.util.Log
import android.os.Bundle
import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.data.media.SimilarScreenshotScanResult
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.feature.photos.PhotoDeletionSummary
import com.air.cleaner.feature.photos.PhotoPostDeleteAction
import com.air.cleaner.feature.photos.PhotoPostDeleteStatus
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
                AnalyticsParameterContract("fingerprint_skipped_count", AnalyticsParameterType.Long),
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
    private val maxEvents: Int = 12,
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
    val completedSteps = similarScreenshotDiagnosticsFunnel.count { it.name in eventNames }
    val furthestStepIndex = similarScreenshotDiagnosticsFunnel.indexOfLast { it.name in eventNames }
    val nextActionLabel = if (furthestStepIndex == -1) {
        "Next: open Photos > Similar photos."
    } else {
        similarScreenshotDiagnosticsFunnel[furthestStepIndex].nextActionLabel
    }
    return AnalyticsDiagnosticsSummary(
        latestEventLabel = firstOrNull()?.let { "Last local event: ${it.name}" } ?: "Last local event: none",
        similarFunnelProgressLabel = "Similar photos funnel: $completedSteps/${similarScreenshotDiagnosticsFunnel.size}",
        similarFunnelNextStepLabel = nextActionLabel,
        similarScanInsightLabels = toSimilarScreenshotScanInsightLabels(),
    )
}

internal fun List<CleanerTelemetryEvent>.toAnalyticsDiagnosticsReport(analyticsEnabled: Boolean): String {
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
        add(summary.similarFunnelProgressLabel)
        add(summary.similarFunnelNextStepLabel)
        addAll(toSimilarScreenshotCacheInsightLabels())
        addAll(summary.similarScanInsightLabels)
        addAll(toSimilarScreenshotReviewInsightLabels())
        add(summary.latestEventLabel)
        add("Recent events:")
        addAll(recentEventLines)
    }.joinToString(separator = "\n")
}

internal fun List<CleanerTelemetryEvent>.toAnalyticsDiagnosticsShareContent(
    analyticsEnabled: Boolean,
): AnalyticsDiagnosticsShareContent {
    return AnalyticsDiagnosticsShareContent(
        title = "AI Photo Cleaner diagnostics",
        text = toAnalyticsDiagnosticsReport(analyticsEnabled = analyticsEnabled),
        unavailableMessage = "No share target found. Diagnostics copied instead.",
    )
}

private data class SimilarScreenshotDiagnosticsStep(
    val name: String,
    val nextActionLabel: String,
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
        diagnosis,
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
                "recoverable_bytes" to groups.orEmpty().sumOf { it.recoverableBytes },
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
                "recoverable_bytes" to groups.sumOf { it.recoverableBytes },
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
                "group_count" to groups.size,
                "recoverable_bytes" to groups.sumOf { it.recoverableBytes },
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
                "recoverable_bytes" to groups.sumOf { it.recoverableBytes },
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

private const val SIMILAR_SCREENSHOT_COLD_SCAN_TARGET_MS = 3_500L

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
