package com.air.cleaner.ui

enum class SimilarScreenshotReviewStatus {
    Loading,
    CachedRefreshing,
    FilteredCacheEmpty,
    Fresh,
}

internal fun SimilarScreenshotReviewStatus.noticeTitle(): String? {
    return when (this) {
        SimilarScreenshotReviewStatus.CachedRefreshing -> "Showing saved results"
        SimilarScreenshotReviewStatus.Loading -> null
        SimilarScreenshotReviewStatus.FilteredCacheEmpty -> null
        SimilarScreenshotReviewStatus.Fresh -> null
    }
}

internal fun SimilarScreenshotReviewStatus.noticeMessage(): String? {
    return when (this) {
        SimilarScreenshotReviewStatus.CachedRefreshing -> {
            "Refreshing in the background. Review stays available while we check for library changes."
        }
        SimilarScreenshotReviewStatus.Loading -> null
        SimilarScreenshotReviewStatus.FilteredCacheEmpty -> null
        SimilarScreenshotReviewStatus.Fresh -> null
    }
}

internal fun SimilarScreenshotReviewStatus.emptyTitle(): String {
    return when (this) {
        SimilarScreenshotReviewStatus.FilteredCacheEmpty -> "Saved results were updated"
        SimilarScreenshotReviewStatus.Loading -> "No similar screenshots found"
        SimilarScreenshotReviewStatus.CachedRefreshing -> "No similar screenshots found"
        SimilarScreenshotReviewStatus.Fresh -> "No similar screenshots found"
    }
}

internal fun SimilarScreenshotReviewStatus.emptyMessage(): String {
    return when (this) {
        SimilarScreenshotReviewStatus.FilteredCacheEmpty -> {
            "Previous matches included photos that no longer exist. We removed stale candidates and are checking your library again."
        }
        SimilarScreenshotReviewStatus.Loading -> {
            "We checked screenshots for near-identical layouts and tiny visual changes. Nothing reached the safe review threshold yet. Try taking 2-3 screenshots of the same screen, then rescan."
        }
        SimilarScreenshotReviewStatus.CachedRefreshing -> {
            "We checked screenshots for near-identical layouts and tiny visual changes. Nothing reached the safe review threshold yet. Try taking 2-3 screenshots of the same screen, then rescan."
        }
        SimilarScreenshotReviewStatus.Fresh -> {
            "We checked screenshots for near-identical layouts and tiny visual changes. Nothing reached the safe review threshold yet. Try taking 2-3 screenshots of the same screen, then rescan."
        }
    }
}

internal fun SimilarScreenshotReviewStatus.emptyActionLabel(): String = "Rescan photos"
