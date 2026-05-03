package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup

data class PhotoPostDeleteStatus(
    val title: String,
    val message: String,
    val remainingGroupCount: Int,
    val remainingRecoverableBytes: Long,
) {
    companion object {
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
