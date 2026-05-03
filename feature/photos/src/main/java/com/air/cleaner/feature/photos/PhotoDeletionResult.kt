package com.air.cleaner.feature.photos

data class PhotoDeletionResult(
    val status: PhotoDeletionStatus,
    val itemCount: Int,
    val bytes: Long,
) {
    val shouldRefreshScan: Boolean = status == PhotoDeletionStatus.Deleted
    val title: String = when (status) {
        PhotoDeletionStatus.Deleted -> "Cleanup complete"
        PhotoDeletionStatus.Canceled -> "Deletion canceled"
        PhotoDeletionStatus.Blocked -> "Deletion needs attention"
    }
    val primaryActionLabel: String = when (status) {
        PhotoDeletionStatus.Deleted -> "Review remaining"
        PhotoDeletionStatus.Canceled,
        PhotoDeletionStatus.Blocked,
        -> "Back to review"
    }

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
