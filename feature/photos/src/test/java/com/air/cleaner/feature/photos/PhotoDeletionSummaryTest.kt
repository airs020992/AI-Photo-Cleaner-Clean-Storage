package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoDeletionSummaryTest {
    @Test
    fun summarizesSelectedItemsBeforeSystemDeleteRequest() {
        val summary = PhotoDeletionSummary.fromItems(
            listOf(
                mediaItem("a", 1_000L, "content://images/a"),
                mediaItem("b", 2_000L, "content://images/b"),
            ),
        )

        assertEquals(2, summary.itemCount)
        assertEquals(3_000L, summary.bytesToDelete)
        assertEquals(listOf("content://images/a", "content://images/b"), summary.contentUris)
        assertTrue(summary.canRequestSystemDelete)
    }

    @Test
    fun exposesDeleteConfirmationTrustCopy() {
        val summary = PhotoDeletionSummary.fromItems(
            listOf(
                mediaItem("a", 1_000L, "content://images/a"),
                mediaItem("b", 2_000L, "content://images/b"),
            ),
        )

        assertEquals("2 selected", summary.itemCountLabel)
        assertEquals("Android confirmation required", summary.systemConfirmationLabel)
        assertEquals("Cancel keeps your current selection", summary.cancelSafetyLabel)
        assertEquals(null, summary.blockedReason)
    }

    @Test
    fun blocksSystemDeleteWhenSelectedItemsDoNotHaveContentUris() {
        val summary = PhotoDeletionSummary.fromItems(
            listOf(mediaItem("a", 1_000L, null)),
        )

        assertEquals(1, summary.itemCount)
        assertEquals(1_000L, summary.bytesToDelete)
        assertTrue(summary.contentUris.isEmpty())
        assertTrue(!summary.canRequestSystemDelete)
        assertEquals("1 selected photo is missing Android delete access.", summary.blockedReason)
    }

    private fun mediaItem(
        id: String,
        sizeBytes: Long,
        contentUri: String?,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = "$id.jpg",
            sizeBytes = sizeBytes,
            dateTakenMillis = 0L,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = contentUri,
        )
    }
}
