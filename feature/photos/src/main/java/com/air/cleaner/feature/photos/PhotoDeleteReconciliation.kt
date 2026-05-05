package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup

data class PhotoDeleteReconciliation(
    val requestedItemCount: Int,
    val requestedBytes: Long,
    val resolvedItemCount: Int,
    val stillExistsCount: Int = 0,
    val stillExistingUris: List<String> = emptyList(),
    val stillNeedsReviewCount: Int,
    val stillNeedsReviewUris: List<String>,
    val remainingGroupCount: Int,
    val remainingRecoverableBytes: Long,
    val remainingHighPriorityGroupCount: Int = 0,
    val remainingMediumPriorityGroupCount: Int = 0,
) {
    val remainingPriorityGroupCount: Int =
        remainingHighPriorityGroupCount + remainingMediumPriorityGroupCount

    companion object {
        fun from(
            summary: PhotoDeletionSummary?,
            result: PhotoDeletionResult?,
            currentGroups: List<DuplicateGroup>,
            stillExistingContentUris: List<String>? = null,
            remainingHighPriorityGroupCount: Int = 0,
            remainingMediumPriorityGroupCount: Int = 0,
            keepStrategy: PhotoReviewKeepStrategy? = null,
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
            val stillExistingUris = stillExistingContentUris.orEmpty().filter { it in requestedUris }
            val unresolvedUris = stillExistingContentUris ?: stillNeedsReviewUris

            return PhotoDeleteReconciliation(
                requestedItemCount = summary.itemCount,
                requestedBytes = summary.bytesToDelete,
                resolvedItemCount = requestedUris.size - unresolvedUris.toSet().size,
                stillExistsCount = stillExistingUris.size,
                stillExistingUris = stillExistingUris,
                stillNeedsReviewCount = stillNeedsReviewUris.size,
                stillNeedsReviewUris = stillNeedsReviewUris,
                remainingGroupCount = currentGroups.size,
                remainingRecoverableBytes = currentGroups.remainingRecoverableBytes(keepStrategy),
                remainingHighPriorityGroupCount = remainingHighPriorityGroupCount,
                remainingMediumPriorityGroupCount = remainingMediumPriorityGroupCount,
            )
        }
    }
}

private fun List<DuplicateGroup>.remainingRecoverableBytes(
    keepStrategy: PhotoReviewKeepStrategy?,
): Long {
    return sumOf { group ->
        if (keepStrategy == null) {
            group.recoverableBytes
        } else {
            val keepId = group.keepItem(keepStrategy).id
            group.items.filterNot { item -> item.id == keepId }.sumOf { item -> item.sizeBytes }
        }
    }
}
