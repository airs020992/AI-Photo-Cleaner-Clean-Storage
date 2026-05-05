package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.feature.photos.PhotoReviewKeepStrategy
import com.air.cleaner.feature.photos.SimilarScreenshotReviewPriority
import com.air.cleaner.feature.photos.similarScreenshotKeepGuidance

enum class PhotoDeleteReviewContext {
    DuplicatePhotos,
    LargeVideos,
    SimilarPhotos,
    SimilarScreenshots,
}

val PhotoDeleteReviewContext.deleteDialogTitle: String
    get() = when (this) {
        PhotoDeleteReviewContext.LargeVideos -> "Delete selected videos?"
        PhotoDeleteReviewContext.DuplicatePhotos,
        PhotoDeleteReviewContext.SimilarPhotos,
        PhotoDeleteReviewContext.SimilarScreenshots -> "Delete selected photos?"
    }

val PhotoDeleteReviewContext.deleteDialogItemLabel: String
    get() = when (this) {
        PhotoDeleteReviewContext.LargeVideos -> "Videos"
        PhotoDeleteReviewContext.DuplicatePhotos,
        PhotoDeleteReviewContext.SimilarPhotos,
        PhotoDeleteReviewContext.SimilarScreenshots -> "Photos"
    }

val PhotoDeleteReviewContext.deleteResultDeletedMessage: String
    get() = when (this) {
        PhotoDeleteReviewContext.LargeVideos ->
            "Your library was refreshed. Continue reviewing any remaining large videos."
        PhotoDeleteReviewContext.DuplicatePhotos,
        PhotoDeleteReviewContext.SimilarPhotos,
        PhotoDeleteReviewContext.SimilarScreenshots ->
            "Your library was refreshed. Continue reviewing any remaining duplicate groups."
    }

val PhotoDeleteReviewContext.deleteResultCanceledMessage: String
    get() = when (this) {
        PhotoDeleteReviewContext.LargeVideos ->
            "No videos were removed. Your previous selection is still available for review."
        PhotoDeleteReviewContext.DuplicatePhotos,
        PhotoDeleteReviewContext.SimilarPhotos,
        PhotoDeleteReviewContext.SimilarScreenshots ->
            "No photos were removed. Your previous selection is still available for review."
    }

fun PhotoDeleteReviewContext.groupsForReconciliation(
    duplicatePhotoGroups: List<DuplicateGroup>,
    similarPhotoGroups: List<DuplicateGroup>,
    similarScreenshotGroups: List<DuplicateGroup>,
): List<DuplicateGroup> {
    return when (this) {
        PhotoDeleteReviewContext.DuplicatePhotos -> duplicatePhotoGroups
        PhotoDeleteReviewContext.LargeVideos -> emptyList()
        PhotoDeleteReviewContext.SimilarPhotos -> similarPhotoGroups
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
