package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import kotlin.math.roundToInt

data class SimilarScreenshotTrustSummary(
    val title: String,
    val lines: List<String>,
)

fun DuplicateGroup.similarScreenshotTrustSummary(
    keepStrategy: PhotoReviewKeepStrategy,
): SimilarScreenshotTrustSummary {
    val keepItem = keepItem(keepStrategy)
    val cleanableItems = items.filterNot { it.id == keepItem.id }
    val cleanableCount = cleanableItems.size
    val cleanablePhotoLabel = if (cleanableCount == 1) "photo" else "photos"
    return SimilarScreenshotTrustSummary(
        title = when (keepStrategy) {
            PhotoReviewKeepStrategy.Newest -> "Suggested keep: newest screenshot"
            PhotoReviewKeepStrategy.Recommended -> "Suggested keep: highest-quality original"
        },
        lines = listOf(
            "Keeps ${keepItem.displayName}",
            "Can clean $cleanableCount $cleanablePhotoLabel | ${formatTrustBytes(cleanableItems.sumOf { it.sizeBytes })}",
            "Confidence: same screen size + visual fingerprint",
            "Range: ${trustCaptureRangeLabel()}",
        ),
    )
}

private fun DuplicateGroup.trustCaptureRangeLabel(): String {
    val rangeMillis = items.maxOf { it.dateTakenMillis } - items.minOf { it.dateTakenMillis }
    val seconds = (rangeMillis / 1_000.0).roundToInt().coerceAtLeast(0)
    if (seconds > 60 * 60) return "different sessions; review carefully"
    if (seconds < 60) return "captured $seconds sec apart"
    return "captured ${(seconds / 60.0).roundToInt()} min apart"
}

private fun formatTrustBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes / (1024.0 * 1024.0)
    if (megabytes < 1024.0) return "${megabytes.roundToInt()} MB"
    return String.format("%.1f GB", megabytes / 1024.0)
}
