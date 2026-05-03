package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup

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
