package com.air.cleaner.ui

import android.util.Log
import com.air.cleaner.data.media.MediaScanSummary
import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.feature.photos.PhotoDeletionSummary

internal data class CleanerTelemetryEvent(
    val name: String,
    val properties: Map<String, Any>,
)

internal interface CleanerTelemetry {
    fun track(event: CleanerTelemetryEvent)
}

internal object NoOpCleanerTelemetry : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) = Unit
}

internal class ConsentAwareCleanerTelemetry(
    private val delegate: CleanerTelemetry,
    private val analyticsEnabled: () -> Boolean,
) : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        if (analyticsEnabled()) {
            delegate.track(event)
        }
    }
}

internal class LogcatCleanerTelemetry : CleanerTelemetry {
    override fun track(event: CleanerTelemetryEvent) {
        Log.d("AIPhotoCleaner", "${event.name} ${event.properties}")
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
}

private val SimilarScreenshotReviewStatus.analyticsValue: String
    get() = name.lowercase()
