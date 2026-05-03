package com.air.cleaner.feature.photos

data class PhotoDeletionResult(
    val status: PhotoDeletionStatus,
    val itemCount: Int,
    val bytes: Long,
) {
    val shouldRefreshScan: Boolean = status == PhotoDeletionStatus.Deleted

    companion object {
        fun fromSystemResult(
            summary: PhotoDeletionSummary,
            systemConfirmed: Boolean,
        ): PhotoDeletionResult {
            return PhotoDeletionResult(
                status = if (systemConfirmed) PhotoDeletionStatus.Deleted else PhotoDeletionStatus.Canceled,
                itemCount = summary.itemCount,
                bytes = summary.bytesToDelete,
            )
        }

        fun blocked(summary: PhotoDeletionSummary): PhotoDeletionResult {
            return PhotoDeletionResult(
                status = PhotoDeletionStatus.Blocked,
                itemCount = summary.itemCount,
                bytes = summary.bytesToDelete,
            )
        }
    }
}

enum class PhotoDeletionStatus {
    Deleted,
    Canceled,
    Blocked,
}
