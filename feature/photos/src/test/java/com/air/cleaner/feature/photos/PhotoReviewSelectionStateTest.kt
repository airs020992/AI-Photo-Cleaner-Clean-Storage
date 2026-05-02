package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoReviewSelectionStateTest {
    @Test
    fun defaultsToKeepingOneItemPerGroupAndSelectingTheRest() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("old", 3_000L, 100L),
                    mediaItem("new", 2_000L, 200L),
                ),
            ),
        )

        assertFalse(state.isSelectedForDeletion("old"))
        assertTrue(state.isSelectedForDeletion("new"))
        assertEquals(1, state.selectedCount)
        assertEquals(2_000L, state.selectedBytes)
    }

    @Test
    fun preventsSelectingEveryItemInAGroupForDeletion() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("keep", 3_000L, 100L),
                    mediaItem("delete", 2_000L, 200L),
                ),
            ),
        )

        val afterToggle = state.toggle("keep")

        assertFalse(afterToggle.isSelectedForDeletion("keep"))
        assertTrue(afterToggle.isSelectedForDeletion("delete"))
        assertEquals(1, afterToggle.selectedCount)
    }

    @Test
    fun allowsUserToDeselectSuggestedDeletion() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("keep", 3_000L, 100L),
                    mediaItem("delete", 2_000L, 200L),
                ),
            ),
        )

        val afterToggle = state.toggle("delete")

        assertFalse(afterToggle.isSelectedForDeletion("keep"))
        assertFalse(afterToggle.isSelectedForDeletion("delete"))
        assertEquals(0, afterToggle.selectedCount)
        assertEquals(0L, afterToggle.selectedBytes)
    }

    private fun duplicateGroup(vararg items: MediaItem): DuplicateGroup {
        return DuplicateGroup(key = "group", items = items.toList())
    }

    private fun mediaItem(id: String, sizeBytes: Long, dateTakenMillis: Long): MediaItem {
        return MediaItem(
            id = id,
            displayName = "$id.jpg",
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            contentHash = id,
            mediaType = MediaType.Image,
        )
    }
}
