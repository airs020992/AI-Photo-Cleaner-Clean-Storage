package com.air.cleaner.ui

enum class SimilarScreenshotReviewStatus {
    Loading,
    CachedRefreshing,
    Fresh,
}

internal fun SimilarScreenshotReviewStatus.noticeTitle(): String? {
    return when (this) {
        SimilarScreenshotReviewStatus.CachedRefreshing -> "Showing saved results"
        SimilarScreenshotReviewStatus.Loading -> null
        SimilarScreenshotReviewStatus.Fresh -> null
    }
}

internal fun SimilarScreenshotReviewStatus.noticeMessage(): String? {
    return when (this) {
        SimilarScreenshotReviewStatus.CachedRefreshing -> {
            "Refreshing in the background. Review stays available while we check for library changes."
        }
        SimilarScreenshotReviewStatus.Loading -> null
        SimilarScreenshotReviewStatus.Fresh -> null
    }
}
