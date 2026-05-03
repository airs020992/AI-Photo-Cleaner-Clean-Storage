package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
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

    @Test
    fun summarizesSelectedSimilarScreenshotPriorityRiskBeforeSystemDeleteRequest() {
        val highRiskGroup = duplicateGroup(
            mediaItem("receipt-keep", 2_000L, "content://images/receipt-keep", "Receipt checkout.png"),
            mediaItem("receipt-delete", 1_000L, "content://images/receipt-delete", "Receipt checkout copy.png"),
            key = "high",
        )
        val mediumRiskGroup = duplicateGroup(
            mediaItem("crop-keep", 2_000L, "content://images/crop-keep", "Screenshot crop keep.png", width = 1440),
            mediaItem("crop-delete", 1_000L, "content://images/crop-delete", "Screenshot crop delete.png", width = 1080),
            key = "medium",
        )
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(highRiskGroup, mediumRiskGroup),
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        val summary = PhotoDeletionSummary.fromSimilarScreenshotSelection(
            groups = listOf(highRiskGroup, mediumRiskGroup),
            selectionState = state,
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertEquals(2, summary.priorityGroupCount)
        assertEquals(1, summary.highPriorityGroupCount)
        assertEquals(1, summary.mediumPriorityGroupCount)
        assertEquals("2 priority groups selected", summary.priorityGroupCountLabel)
        assertEquals(
            "Review again: 1 high risk group and 1 medium risk group are selected.",
            summary.priorityWarningLine,
        )
    }

    @Test
    fun hidesPriorityRiskCopyWhenOnlyNormalSimilarScreenshotGroupsAreSelected() {
        val normalGroup = duplicateGroup(
            mediaItem("keep", 2_000L, "content://images/keep", "Screenshot keep.png"),
            mediaItem("delete", 1_000L, "content://images/delete", "Screenshot delete.png"),
            key = "normal",
        )
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(normalGroup),
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        val summary = PhotoDeletionSummary.fromSimilarScreenshotSelection(
            groups = listOf(normalGroup),
            selectionState = state,
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertEquals(0, summary.priorityGroupCount)
        assertEquals(null, summary.priorityGroupCountLabel)
        assertEquals(null, summary.priorityWarningLine)
    }

    @Test
    fun omitsZeroCountPriorityTypesFromRiskCopy() {
        val highRiskGroup = duplicateGroup(
            mediaItem("receipt-keep", 2_000L, "content://images/receipt-keep", "Receipt checkout.png"),
            mediaItem("receipt-delete", 1_000L, "content://images/receipt-delete", "Receipt checkout copy.png"),
            key = "high",
        )
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(highRiskGroup),
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        val summary = PhotoDeletionSummary.fromSimilarScreenshotSelection(
            groups = listOf(highRiskGroup),
            selectionState = state,
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertEquals(
            "Review again: 1 high risk group is selected.",
            summary.priorityWarningLine,
        )
    }

    private fun duplicateGroup(
        vararg items: MediaItem,
        key: String,
    ): DuplicateGroup {
        return DuplicateGroup(key = key, items = items.toList())
    }

    private fun mediaItem(
        id: String,
        sizeBytes: Long,
        contentUri: String?,
        displayName: String = "$id.jpg",
        width: Int? = 1440,
        height: Int? = 3120,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = 0L,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = contentUri,
            width = width,
            height = height,
        )
    }
}
