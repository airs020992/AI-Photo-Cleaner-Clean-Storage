package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.MediaItem

data class PhotoDeletionSummary(
    val itemCount: Int,
    val bytesToDelete: Long,
    val contentUris: List<String>,
) {
    val canRequestSystemDelete: Boolean = itemCount > 0 && contentUris.size == itemCount
    val missingDeleteAccessCount: Int = itemCount - contentUris.size
    val itemCountLabel: String = "$itemCount selected"
    val systemConfirmationLabel: String = "Android confirmation required"
    val cancelSafetyLabel: String = "Cancel keeps your current selection"
    val blockedReason: String? = if (missingDeleteAccessCount > 0) {
        "$missingDeleteAccessCount selected photo${if (missingDeleteAccessCount == 1) "" else "s"} is missing Android delete access."
    } else {
        null
    }

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
