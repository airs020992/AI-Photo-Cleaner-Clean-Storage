package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.MediaItem

data class PhotoDeletionSummary(
    val itemCount: Int,
    val bytesToDelete: Long,
    val contentUris: List<String>,
) {
    val canRequestSystemDelete: Boolean = itemCount > 0 && contentUris.size == itemCount

    companion object {
        fun fromItems(items: List<MediaItem>): PhotoDeletionSummary {
            return PhotoDeletionSummary(
                itemCount = items.size,
                bytesToDelete = items.sumOf { it.sizeBytes },
                contentUris = items.mapNotNull { it.contentUri },
            )
        }
    }
}
