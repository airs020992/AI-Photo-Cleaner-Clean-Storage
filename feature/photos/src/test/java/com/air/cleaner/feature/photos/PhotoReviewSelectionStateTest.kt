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
    fun disablesContinueWhenNothingIsSelectedForDeletion() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("keep", 3_000L, 100L),
                    mediaItem("delete", 2_000L, 200L),
                ),
            ),
        )

        val afterDeselect = state.toggle("delete")

        assertFalse(afterDeselect.canContinue)
    }

    @Test
    fun canDeselectOneGroupWithoutChangingOtherGroups() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("first-keep", 3_000L, 100L),
                    mediaItem("first-delete", 2_000L, 200L),
                    key = "first",
                ),
                duplicateGroup(
                    mediaItem("second-keep", 4_000L, 300L),
                    mediaItem("second-delete", 1_000L, 400L),
                    key = "second",
                ),
            ),
        )

        val afterDeselect = state.deselectGroup("first")

        assertEquals(0, afterDeselect.selectedCountInGroup("first"))
        assertEquals(0L, afterDeselect.selectedBytesInGroup("first"))
        assertEquals(1, afterDeselect.selectedCountInGroup("second"))
        assertEquals(1_000L, afterDeselect.selectedBytesInGroup("second"))
        assertFalse(afterDeselect.isSelectedForDeletion("first-keep"))
        assertFalse(afterDeselect.isSelectedForDeletion("first-delete"))
        assertFalse(afterDeselect.isSelectedForDeletion("second-keep"))
        assertTrue(afterDeselect.isSelectedForDeletion("second-delete"))
        assertEquals(1, afterDeselect.selectedCount)
        assertEquals(1_000L, afterDeselect.selectedBytes)
    }

    @Test
    fun canResetOneGroupToSuggestedDeletion() {
        val group = duplicateGroup(
            mediaItem("older", 3_000L, 100L),
            mediaItem("newest", 2_000L, 200L),
            key = "screenshots",
        )
        val state = PhotoReviewSelectionState
            .fromGroups(listOf(group), keepStrategy = PhotoReviewKeepStrategy.Newest)
            .deselectGroup("screenshots")

        val afterReset = state.resetGroup("screenshots", keepStrategy = PhotoReviewKeepStrategy.Newest)

        assertTrue(afterReset.isSelectedForDeletion("older"))
        assertFalse(afterReset.isSelectedForDeletion("newest"))
        assertEquals(1, afterReset.selectedCountInGroup("screenshots"))
        assertEquals(3_000L, afterReset.selectedBytesInGroup("screenshots"))
        assertEquals(1, afterReset.selectedCount)
        assertEquals(3_000L, afterReset.selectedBytes)
    }

    @Test
    fun exposesNewestPreviewItemsPerGroup() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("oldest", 1_000L, 100L),
                    mediaItem("newest", 1_000L, 500L),
                    mediaItem("middle", 1_000L, 300L),
                    mediaItem("newer", 1_000L, 400L),
                    mediaItem("older", 1_000L, 200L),
                    key = "screenshots",
                ),
            ),
        )

        assertEquals(
            listOf("newest", "newer", "middle", "older"),
            state.previewItemsInGroup("screenshots").map { it.id },
        )
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

    @Test
    fun canProtectReviewGroupsFromInitialDeletionSelection() {
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("priority-keep", 3_000L, 100L),
                    mediaItem("priority-delete", 2_000L, 200L),
                    key = "priority",
                ),
                duplicateGroup(
                    mediaItem("normal-keep", 4_000L, 300L),
                    mediaItem("normal-delete", 1_000L, 400L),
                    key = "normal",
                ),
            ),
            protectedGroupKeys = setOf("priority"),
        )

        assertFalse(state.isSelectedForDeletion("priority-keep"))
        assertFalse(state.isSelectedForDeletion("priority-delete"))
        assertFalse(state.isSelectedForDeletion("normal-keep"))
        assertTrue(state.isSelectedForDeletion("normal-delete"))
        assertEquals(1, state.selectedCount)
        assertEquals(1_000L, state.selectedBytes)
    }

    @Test
    fun similarScreenshotsDefaultToActionableSuggestedDeletionForReviewGroups() {
        val state = PhotoReviewSelectionState.fromSimilarScreenshotGroups(
            groups = listOf(
                duplicateGroup(
                    mediaItem("older-screenshot", 3_000L, 100L),
                    mediaItem("newest-screenshot", 2_000L, 200L),
                    key = "priority",
                ),
            ),
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertTrue(state.isSelectedForDeletion("older-screenshot"))
        assertFalse(state.isSelectedForDeletion("newest-screenshot"))
        assertEquals(1, state.selectedCount)
        assertEquals(3_000L, state.selectedBytes)
        assertTrue(state.canContinue)
    }

    private fun duplicateGroup(
        vararg items: MediaItem,
        key: String = "group",
    ): DuplicateGroup {
        return DuplicateGroup(key = key, items = items.toList())
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
