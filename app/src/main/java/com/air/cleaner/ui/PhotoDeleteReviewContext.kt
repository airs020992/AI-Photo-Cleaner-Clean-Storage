package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.feature.photos.PhotoReviewKeepStrategy
import com.air.cleaner.feature.photos.SimilarScreenshotReviewPriority
import com.air.cleaner.feature.photos.similarScreenshotKeepGuidance

enum class PhotoDeleteReviewContext {
    DuplicatePhotos,
    SimilarScreenshots,
}

fun PhotoDeleteReviewContext.groupsForReconciliation(
    duplicatePhotoGroups: List<DuplicateGroup>,
    similarScreenshotGroups: List<DuplicateGroup>,
): List<DuplicateGroup> {
    return when (this) {
        PhotoDeleteReviewContext.DuplicatePhotos -> duplicatePhotoGroups
        PhotoDeleteReviewContext.SimilarScreenshots -> similarScreenshotGroups
    }
}

fun PhotoDeleteReviewContext.remainingHighPriorityGroups(
    similarScreenshotGroups: List<DuplicateGroup>,
): Int {
    return remainingPriorityGroups(
        similarScreenshotGroups = similarScreenshotGroups,
        priority = SimilarScreenshotReviewPriority.High,
    )
}

fun PhotoDeleteReviewContext.remainingMediumPriorityGroups(
    similarScreenshotGroups: List<DuplicateGroup>,
): Int {
    return remainingPriorityGroups(
        similarScreenshotGroups = similarScreenshotGroups,
        priority = SimilarScreenshotReviewPriority.Medium,
    )
}

private fun PhotoDeleteReviewContext.remainingPriorityGroups(
    similarScreenshotGroups: List<DuplicateGroup>,
    priority: SimilarScreenshotReviewPriority,
): Int {
    if (this != PhotoDeleteReviewContext.SimilarScreenshots) {
        return 0
    }
    return similarScreenshotGroups.count { group ->
        group.similarScreenshotKeepGuidance(PhotoReviewKeepStrategy.Newest).reviewPriority == priority
    }
}
