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

        val dateMillis = row.dateTakenMillis ?: row.dateModifiedSeconds.toMillis()
        return MediaItem(
            id = row.id.toString(),
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = dateMillis,
            contentHash = null,
            mediaType = mediaType,
            relativePath = row.relativePath?.trim()?.takeIf { it.isNotEmpty() },
            width = row.width?.takeIf { it > 0 },
            height = row.height?.takeIf { it > 0 },
            durationMillis = row.durationMillis?.takeIf { it > 0 },
        )
    }

    private fun Long?.toMillis(): Long {
        return this?.times(1_000L) ?: 0L
    }
}
