package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType

class LargeVideoScanner(
    private val topCount: Int = 20,
) {
    fun findLargeVideos(items: List<MediaItem>): List<MediaItem> {
        return items
            .asSequence()
            .filter { item -> item.mediaType == MediaType.Video }
            .sortedByDescending { item -> item.sizeBytes }
            .take(topCount.coerceAtLeast(0))
            .toList()
    }
}
