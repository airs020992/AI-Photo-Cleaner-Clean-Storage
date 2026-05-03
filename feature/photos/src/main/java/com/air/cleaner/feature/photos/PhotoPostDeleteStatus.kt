package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup

data class PhotoPostDeleteStatus(
    val title: String,
    val message: String,
    val remainingGroupCount: Int,
    val remainingRecoverableBytes: Long,
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
                    reconciliation.stillNeedsReviewCount > 0 -> "${reconciliation.stillNeedsReviewCount} selected ${if (reconciliation.stillNeedsReviewCount == 1) "photo still appears" else "photos still appear"} in duplicate review"
                    else -> "${reconciliation.remainingGroupCount} duplicate ${if (reconciliation.remainingGroupCount == 1) "group" else "groups"} still ${if (reconciliation.remainingGroupCount == 1) "needs" else "need"} review"
                },
                remainingGroupCount = reconciliation.remainingGroupCount,
                remainingRecoverableBytes = reconciliation.remainingRecoverableBytes,
            )
        }

        private fun photoNoun(count: Int): String {
            return if (count == 1) "photo" else "photos"
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
