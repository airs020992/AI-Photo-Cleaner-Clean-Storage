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
        assertEquals("Keep this photo", detail.decisionTitle)
        assertEquals("This is the copy AI Photo Cleaner recommends keeping. It will not be deleted unless you change the selection.", detail.decisionBody)
        assertEquals("Select for deletion", detail.decisionActionLabel)
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
        assertEquals("Ready to delete", detail.decisionTitle)
        assertEquals("This photo is selected for cleanup. Android will ask for final confirmation before anything is removed.", detail.decisionBody)
        assertEquals("Keep this", detail.decisionActionLabel)
        assertEquals("Size: 2 MB | dimensions unavailable", detail.metadataLine)
    }

    @Test
    fun unselectedCandidateExplainsManualReviewStatus() {
        val detail = mediaItem(
            id = "candidate",
            displayName = "Checkout confirmation alt.png",
            sizeBytes = 3_145_728L,
            width = 1080,
            height = 2400,
        ).toPhotoPreviewDetail(
            isRecommendedKeep = false,
            selectedForDeletion = false,
            itemMatchLabel = "Similar screenshot",
        )

        assertEquals("Similar screenshot | 3 MB", detail.roleLine)
        assertEquals("Status: kept", detail.statusLine)
        assertEquals("Not selected", detail.decisionTitle)
        assertEquals("This similar photo is currently protected. Select it only if the larger preview confirms it is safe to remove.", detail.decisionBody)
        assertEquals("Select for deletion", detail.decisionActionLabel)
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
