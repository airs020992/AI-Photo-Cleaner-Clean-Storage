package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoPreviewSessionTest {
    @Test
    fun ordersGroupItemsNewestFirstAndExposesNavigationState() {
        val session = listOf(
            mediaItem("oldest", 100L),
            mediaItem("newest", 500L),
            mediaItem("middle", 300L),
        ).toPhotoPreviewSession(currentItemId = "middle")

        assertEquals("middle", session.currentItem.id)
        assertEquals("Photo 2 of 3", session.positionLine)
        assertTrue(session.hasPrevious)
        assertTrue(session.hasNext)
        assertEquals("newest", session.previousItem.id)
        assertEquals("oldest", session.nextItem.id)
    }

    @Test
    fun clampsUnknownItemToFirstItemInGroup() {
        val session = listOf(
            mediaItem("old", 100L),
            mediaItem("new", 200L),
        ).toPhotoPreviewSession(currentItemId = "missing")

        assertEquals("new", session.currentItem.id)
        assertEquals("Photo 1 of 2", session.positionLine)
        assertFalse(session.hasPrevious)
        assertTrue(session.hasNext)
        assertEquals("old", session.nextItem.id)
    }

    private fun mediaItem(id: String, dateTakenMillis: Long): MediaItem {
        return MediaItem(
            id = id,
            displayName = "$id.png",
            sizeBytes = 1_000L,
            dateTakenMillis = dateTakenMillis,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = "content://media/$id",
        )
    }
}
