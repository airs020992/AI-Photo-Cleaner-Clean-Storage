package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import kotlin.math.roundToInt

data class PhotoPostDeleteStatus(
    val title: String,
    val message: String,
    val remainingGroupCount: Int,
    val remainingRecoverableBytes: Long,
    val metrics: List<PhotoPostDeleteMetric> = emptyList(),
    val nextActionLabel: String? = null,
    val nextAction: PhotoPostDeleteAction? = null,
) {
    companion object {
        fun from(reconciliation: PhotoDeleteReconciliation?): PhotoPostDeleteStatus? {
            if (reconciliation == null) {
                return null
            }

            return PhotoPostDeleteStatus(
                title = when {
                    reconciliation.stillExistsCount > 0 -> "${reconciliation.resolvedItemCount} ${photoNoun(reconciliation.resolvedItemCount)} deleted"
                    reconciliation.remainingGroupCount == 0 -> "All duplicate photos cleared"
                    reconciliation.stillNeedsReviewCount > 0 -> "${reconciliation.resolvedItemCount} ${photoNoun(reconciliation.resolvedItemCount)} resolved"
                    else -> "${reconciliation.resolvedItemCount} ${photoNoun(reconciliation.resolvedItemCount)} removed"
                },
                message = when {
                    reconciliation.stillExistsCount > 0 -> "${reconciliation.stillExistsCount} selected ${photoNoun(reconciliation.stillExistsCount)} still ${if (reconciliation.stillExistsCount == 1) "exists" else "exist"} in your library"
                    reconciliation.remainingGroupCount == 0 -> "${reconciliation.resolvedItemCount} photos no longer appear in duplicate review"
                    reconciliation.remainingPriorityGroupCount > 0 -> "${reconciliation.remainingPriorityGroupCount} priority ${if (reconciliation.remainingPriorityGroupCount == 1) "group" else "groups"} still ${if (reconciliation.remainingPriorityGroupCount == 1) "needs" else "need"} review"
                    reconciliation.stillNeedsReviewCount > 0 -> "${reconciliation.stillNeedsReviewCount} selected ${if (reconciliation.stillNeedsReviewCount == 1) "photo still appears" else "photos still appear"} in duplicate review"
                    else -> "${reconciliation.remainingGroupCount} duplicate ${if (reconciliation.remainingGroupCount == 1) "group" else "groups"} still ${if (reconciliation.remainingGroupCount == 1) "needs" else "need"} review"
                },
                remainingGroupCount = reconciliation.remainingGroupCount,
                remainingRecoverableBytes = reconciliation.remainingRecoverableBytes,
                metrics = reconciliation.metrics(),
                nextActionLabel = reconciliation.nextAction().label,
                nextAction = reconciliation.nextAction(),
            )
        }

        private fun PhotoDeleteReconciliation.nextAction(): PhotoPostDeleteAction {
            return when {
                remainingPriorityGroupCount > 0 -> PhotoPostDeleteAction.ReviewPriorityGroups
                remainingGroupCount > 0 -> PhotoPostDeleteAction.ReviewRemainingGroups
                else -> PhotoPostDeleteAction.ReturnToPhotos
            }
        }

        private fun PhotoDeleteReconciliation.metrics(): List<PhotoPostDeleteMetric> {
            val baseMetrics = listOf(
                PhotoPostDeleteMetric("Requested", requestedItemCount.toString()),
                PhotoPostDeleteMetric("Deleted", resolvedItemCount.toString()),
                PhotoPostDeleteMetric("Freed", formatDeletedBytes(requestedBytes)),
                PhotoPostDeleteMetric("Still exists", stillExistsCount.toString()),
                PhotoPostDeleteMetric(
                    "Remaining duplicates",
                    "$remainingGroupCount ${if (remainingGroupCount == 1) "group" else "groups"}",
                ),
            )
            if (remainingPriorityGroupCount == 0) {
                return baseMetrics
            }
            return baseMetrics + PhotoPostDeleteMetric(
                "Priority remaining",
                "$remainingPriorityGroupCount ${if (remainingPriorityGroupCount == 1) "group" else "groups"}",
            )
        }

        private fun photoNoun(count: Int): String {
            return if (count == 1) "photo" else "photos"
        }

        private fun formatDeletedBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 MB"
            val kilobytes = bytes / 1024.0
            if (kilobytes < 1024.0) return "${kilobytes.roundToInt()} KB"
            val megabytes = kilobytes / 1024.0
            if (megabytes < 1024.0) return "${megabytes.roundToInt()} MB"
            return String.format("%.1f GB", megabytes / 1024.0)
        }

        fun from(
            result: PhotoDeletionResult?,
            currentGroups: List<DuplicateGroup>,
        ): PhotoPostDeleteStatus? {
            if (result?.status != PhotoDeletionStatus.Deleted) {
                return null
            }

            val remainingGroupCount = currentGroups.size
            val remainingRecoverableBytes = currentGroups.sumOf { it.recoverableBytes }
            return PhotoPostDeleteStatus(
                title = if (remainingGroupCount == 0) {
                    "All duplicate photos cleared"
                } else {
                    "${result.itemCount} photos removed"
                },
                message = if (remainingGroupCount == 0) {
                    "${result.itemCount} photos removed from this cleanup"
                } else {
                    "$remainingGroupCount duplicate ${if (remainingGroupCount == 1) "group" else "groups"} still ${if (remainingGroupCount == 1) "needs" else "need"} review"
                },
                remainingGroupCount = remainingGroupCount,
                remainingRecoverableBytes = remainingRecoverableBytes,
            )
        }
    }
}

data class PhotoPostDeleteMetric(
    val label: String,
    val value: String,
)

enum class PhotoPostDeleteAction(val label: String) {
    ReturnToPhotos("Return to Photos"),
    ReviewRemainingGroups("Review remaining groups"),
    ReviewPriorityGroups("Review priority groups next"),
}
