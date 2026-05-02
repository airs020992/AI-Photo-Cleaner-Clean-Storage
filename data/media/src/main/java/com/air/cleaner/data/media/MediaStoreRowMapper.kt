package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType

object MediaStoreRowMapper {
    fun map(row: MediaStoreRow, mediaType: MediaType): MediaItem? {
        val displayName = row.displayName?.trim().orEmpty()
        val sizeBytes = row.sizeBytes ?: return null
        if (displayName.isEmpty() || sizeBytes <= 0L) {
            return null
        }

        return MediaItem(
            id = row.id.toString(),
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = row.dateTakenMillis ?: row.dateModifiedSeconds.toMillis(),
            contentHash = null,
            mediaType = mediaType,
        )
    }

    private fun Long?.toMillis(): Long {
        return this?.times(1_000L) ?: 0L
    }
}
