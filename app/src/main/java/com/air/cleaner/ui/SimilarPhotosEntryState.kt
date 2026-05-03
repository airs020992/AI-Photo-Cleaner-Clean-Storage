package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup

internal data class SimilarPhotosEntryState(
    val subtitle: String,
    val metric: String,
)

internal fun similarPhotosEntryState(
    groups: List<DuplicateGroup>?,
    reviewStatus: SimilarScreenshotReviewStatus,
    screenshotCount: Int?,
    formatBytes: (Long) -> String,
): SimilarPhotosEntryState {
    val metric = when {
        groups == null && screenshotCount != null -> "$screenshotCount screenshots"
        groups == null -> "Scanning"
        groups.isEmpty() && screenshotCount == 0 -> "0 screenshots"
        groups.isEmpty() -> "0 found"
        else -> formatBytes(groups.sumOf { it.recoverableBytes })
    }
    val subtitle = when {
        groups == null && screenshotCount != null ->
            "Checking $screenshotCount screenshots for near-identical captures"
        groups == null -> "Checking screenshots for near-identical captures"
        groups.isNotEmpty() && reviewStatus == SimilarScreenshotReviewStatus.CachedRefreshing ->
            "Saved results ready; refreshing in background"
        groups.isNotEmpty() -> "Near-identical screenshots ready for review"
        screenshotCount == 0 -> "No screenshots in access scope. Capture or allow photos, then rescan."
        reviewStatus == SimilarScreenshotReviewStatus.FilteredCacheEmpty ->
            "Saved matches were stale. Checking your library again."
        else -> "No safe matches yet. Try 2-3 screenshots of the same screen."
    }
    return SimilarPhotosEntryState(subtitle = subtitle, metric = metric)
}
