package com.air.cleaner.ui

enum class SimilarScreenshotReviewStatus {
    Loading,
    CachedRefreshing,
    FilteredCacheEmpty,
    Fresh,
}

internal data class SimilarScreenshotReviewActionState(
    val title: String,
    val message: String,
    val actionLabel: String,
    val actionEnabled: Boolean,
)

internal fun SimilarScreenshotReviewStatus.reviewActionState(
    hasGroups: Boolean,
): SimilarScreenshotReviewActionState? {
    if (!hasGroups) return null
    return when (this) {
        SimilarScreenshotReviewStatus.CachedRefreshing -> SimilarScreenshotReviewActionState(
            title = "Refreshing results",
            message = "Saved groups stay visible while we check your latest screenshots.",
            actionLabel = "Refreshing",
            actionEnabled = false,
        )
        SimilarScreenshotReviewStatus.Fresh -> SimilarScreenshotReviewActionState(
            title = "Took new screenshots?",
            message = "Scan again before deleting to include the latest captures.",
            actionLabel = "Scan again",
            actionEnabled = true,
        )
        SimilarScreenshotReviewStatus.Loading,
        SimilarScreenshotReviewStatus.FilteredCacheEmpty -> null
    }
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

internal fun SimilarScreenshotReviewStatus.emptyTitle(scanStatus: MediaScanStatus): String {
    val screenshotCount = scanStatus.summary?.screenshotCount
    if (this != SimilarScreenshotReviewStatus.FilteredCacheEmpty && screenshotCount == 0) {
        return "No screenshots to compare"
    }
    return emptyTitle()
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

internal fun SimilarScreenshotReviewStatus.emptyMessage(scanStatus: MediaScanStatus): String {
    val screenshotCount = scanStatus.summary?.screenshotCount
    return when (this) {
        SimilarScreenshotReviewStatus.FilteredCacheEmpty -> {
            val scope = if (screenshotCount == null) {
                "Current scan: screenshot scope is still being counted."
            } else {
                "Current scan: $screenshotCount screenshots in scope."
            }
            "${emptyMessage()}\n\n$scope"
        }
        SimilarScreenshotReviewStatus.Loading,
        SimilarScreenshotReviewStatus.CachedRefreshing,
        SimilarScreenshotReviewStatus.Fresh -> {
            if (screenshotCount == null) {
                emptyMessage()
            } else if (screenshotCount == 0) {
                "We found 0 screenshots in the current photo access scope.\n\nWhat to try: take a few screenshots of the same screen, or allow full photo access, then tap Rescan photos."
            } else {
                "We checked $screenshotCount screenshots for near-identical layouts and tiny visual changes. No safe review groups passed the confidence threshold yet.\n\nWhat to try: take 2-3 screenshots of the same screen, then tap Rescan photos."
            }
        }
    }
}

internal fun SimilarScreenshotReviewStatus.emptyActionLabel(): String = "Rescan photos"
