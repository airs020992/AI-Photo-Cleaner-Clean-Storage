package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.MediaItem

data class PhotoPreviewSession(
    val items: List<MediaItem>,
    val currentIndex: Int,
) {
    val currentItem: MediaItem = items[currentIndex]
    val positionLine: String = "Photo ${currentIndex + 1} of ${items.size}"
    val hasPrevious: Boolean = currentIndex > 0
    val hasNext: Boolean = currentIndex < items.lastIndex
    val previousItem: MediaItem = items[(currentIndex - 1).coerceAtLeast(0)]
    val nextItem: MediaItem = items[(currentIndex + 1).coerceAtMost(items.lastIndex)]
}

fun List<MediaItem>.toPhotoPreviewSession(currentItemId: String): PhotoPreviewSession {
    require(isNotEmpty()) { "Photo preview requires at least one item." }

    val orderedItems = sortedByDescending { it.dateTakenMillis }
    val currentIndex = orderedItems
        .indexOfFirst { it.id == currentItemId }
        .takeIf { it >= 0 }
        ?: 0

    return PhotoPreviewSession(
        items = orderedItems,
        currentIndex = currentIndex,
    )
}
