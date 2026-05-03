package com.air.cleaner.ui

import android.util.Log
import android.os.Bundle
import com.air.cleaner.data.media.MediaScanSummary
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
                AnalyticsParameterContract("group_count", AnalyticsParameterType.Long),
                AnalyticsParameterContract("recoverable_bytes", AnalyticsParameterType.Long),
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
    private val maxEvents: Int = 5,
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
        groups: List<DuplicateGroup>,
        status: SimilarScreenshotReviewStatus,
    ): CleanerTelemetryEvent {
        return CleanerTelemetryEvent(
            name = "similar_screenshots_scan_completed",
            properties = mapOf(
                "elapsed_ms" to elapsedMillis.coerceAtLeast(0L),
                "screenshot_count" to (scanSummary?.screenshotCount ?: 0),
                "group_count" to groups.size,
                "recoverable_bytes" to groups.sumOf { it.recoverableBytes },
                "status" to status.analyticsValue,
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

private val SimilarScreenshotReviewStatus.analyticsValue: String
    get() = name.lowercase()

private val PhotoPostDeleteAction.analyticsValue: String
    get() = when (this) {
        PhotoPostDeleteAction.ReturnToPhotos -> "return_to_photos"
        PhotoPostDeleteAction.ReviewRemainingGroups -> "review_remaining_groups"
        PhotoPostDeleteAction.ReviewPriorityGroups -> "review_priority_groups"
    }
