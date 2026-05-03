package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup

data class PhotoDeleteReconciliation(
    val requestedItemCount: Int,
    val requestedBytes: Long,
    val resolvedItemCount: Int,
    val stillNeedsReviewCount: Int,
    val stillNeedsReviewUris: List<String>,
    val remainingGroupCount: Int,
    val remainingRecoverableBytes: Long,
) {
    companion object {
        fun from(
            summary: PhotoDeletionSummary?,
            result: PhotoDeletionResult?,
            currentGroups: List<DuplicateGroup>,
        ): PhotoDeleteReconciliation? {
            if (summary == null || result?.status != PhotoDeletionStatus.Deleted) {
                return null
            }

            val requestedUris = summary.contentUris.toSet()
            val currentDuplicateUris = currentGroups
                .flatMap { group -> group.items }
                .mapNotNull { item -> item.contentUri }
                .toSet()
            val stillNeedsReviewUris = summary.contentUris.filter { it in currentDuplicateUris }

            return PhotoDeleteReconciliation(
                requestedItemCount = summary.itemCount,
                requestedBytes = summary.bytesToDelete,
                resolvedItemCount = requestedUris.size - stillNeedsReviewUris.toSet().size,
                stillNeedsReviewCount = stillNeedsReviewUris.size,
                stillNeedsReviewUris = stillNeedsReviewUris,
                remainingGroupCount = currentGroups.size,
                remainingRecoverableBytes = currentGroups.sumOf { it.recoverableBytes },
            )
        }
    }
}
