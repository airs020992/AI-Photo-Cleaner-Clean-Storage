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
    fun defaultsToKeepingTheGroupRecommendedItem() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("small-old", 1_000L, 100L),
                    mediaItem("large-new", 3_000L, 200L),
                    mediaItem("medium-newer", 2_000L, 300L),
                ),
            ),
        )

        assertTrue(state.isSelectedForDeletion("small-old"))
        assertFalse(state.isSelectedForDeletion("large-new"))
        assertTrue(state.isSelectedForDeletion("medium-newer"))
        assertEquals(2, state.selectedCount)
        assertEquals(3_000L, state.selectedBytes)
    }

    @Test
    fun canDefaultToKeepingTheNewestItem() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("larger-old", 3_000L, 100L),
                    mediaItem("smaller-new", 2_000L, 200L),
                ),
            ),
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertTrue(state.isSelectedForDeletion("larger-old"))
        assertFalse(state.isSelectedForDeletion("smaller-new"))
        assertEquals(1, state.selectedCount)
        assertEquals(3_000L, state.selectedBytes)
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

    @Test
    fun exposesSelectedItemsForDeleteConfirmation() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("keep", 3_000L, 100L, "content://images/keep"),
                    mediaItem("delete-a", 2_000L, 200L, "content://images/delete-a"),
                    mediaItem("delete-b", 1_000L, 300L, "content://images/delete-b"),
                ),
            ),
        )

        assertEquals(listOf("delete-a", "delete-b"), state.selectedItems.map { it.id }.sorted())
        assertEquals(
            listOf("content://images/delete-a", "content://images/delete-b"),
            state.selectedContentUris.sorted(),
        )
    }

    private fun duplicateGroup(vararg items: MediaItem): DuplicateGroup {
        return DuplicateGroup(key = "group", items = items.toList())
    }

    private fun mediaItem(
        id: String,
        sizeBytes: Long,
        dateTakenMillis: Long,
        contentUri: String? = null,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = "$id.jpg",
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = contentUri,
        )
    }
}
