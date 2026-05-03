package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoPreviewDetailTest {
    @Test
    fun recommendedKeepExplainsProtectedStatus() {
        val detail = mediaItem(
            id = "keep",
            displayName = "Checkout confirmation.png",
            sizeBytes = 2_097_152L,
            width = 1440,
            height = 3120,
        ).toPhotoPreviewDetail(
            isRecommendedKeep = true,
            selectedForDeletion = false,
            itemMatchLabel = "Similar screenshot",
        )

        assertEquals("Preview photo", detail.title)
        assertEquals("Checkout confirmation.png", detail.fileLine)
        assertEquals("Recommended keep | 2 MB", detail.roleLine)
        assertEquals("Status: kept", detail.statusLine)
        assertEquals("Size: 2 MB | 1440 x 3120", detail.metadataLine)
    }

    @Test
    fun selectedCandidateExplainsDeletionStatus() {
        val detail = mediaItem(
            id = "delete",
            displayName = "Checkout confirmation copy.png",
            sizeBytes = 1_572_864L,
            width = null,
            height = null,
        ).toPhotoPreviewDetail(
            isRecommendedKeep = false,
            selectedForDeletion = true,
            itemMatchLabel = "Similar screenshot",
        )

        assertEquals("Checkout confirmation copy.png", detail.fileLine)
        assertEquals("Similar screenshot | 2 MB", detail.roleLine)
        assertEquals("Status: selected for deletion", detail.statusLine)
        assertEquals("Size: 2 MB | dimensions unavailable", detail.metadataLine)
    }

    private fun mediaItem(
        id: String,
        displayName: String,
        sizeBytes: Long,
        width: Int?,
        height: Int?,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = 100L,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = "content://media/$id",
            width = width,
            height = height,
        )
    }
}
