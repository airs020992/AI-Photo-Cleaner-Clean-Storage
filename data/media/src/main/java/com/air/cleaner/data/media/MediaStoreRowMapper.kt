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
            contentHash = row.duplicateCandidateKey(mediaType, sizeBytes, dateMillis),
            mediaType = mediaType,
        )
    }

    private fun Long?.toMillis(): Long {
        return this?.times(1_000L) ?: 0L
    }

    private fun MediaStoreRow.duplicateCandidateKey(
        mediaType: MediaType,
        sizeBytes: Long,
        dateMillis: Long,
    ): String? {
        if (mediaType != MediaType.Image || dateMillis <= 0L) {
            return null
        }
        return "image:$sizeBytes:$dateMillis"
    }
}
