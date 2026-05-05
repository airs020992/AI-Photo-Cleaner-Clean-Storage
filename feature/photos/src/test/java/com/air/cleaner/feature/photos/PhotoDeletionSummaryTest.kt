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
    fun summarizesSelectedGroupsForDeleteConfirmationReview() {
        val highRiskGroup = duplicateGroup(
            mediaItem("receipt-keep", 2_000L, "content://images/receipt-keep", "Receipt keep.png"),
            mediaItem("receipt-delete", 1_000L, "content://images/receipt-delete", "Receipt delete.png"),
            key = "receipt",
        )
        val normalGroup = duplicateGroup(
            mediaItem("test-keep", 2_000L, "content://images/test-keep", "AIRTEST-4.png", dateTakenMillis = 400L),
            mediaItem("test-delete-1", 1_000L, "content://images/test-delete-1", "AIRTEST-1.png", dateTakenMillis = 100L),
            mediaItem("test-delete-2", 2_000L, "content://images/test-delete-2", "AIRTEST-2.png", dateTakenMillis = 200L),
            mediaItem("test-delete-3", 3_000L, "content://images/test-delete-3", "AIRTEST-3.png", dateTakenMillis = 300L),
            mediaItem("test-delete-4", 4_000L, "content://images/test-delete-4", "AIRTEST-0.png", dateTakenMillis = 50L),
            key = "test",
        )
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(highRiskGroup, normalGroup),
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        val summary = PhotoDeletionSummary.fromSimilarScreenshotSelection(
            groups = listOf(highRiskGroup, normalGroup),
            selectionState = state,
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertEquals(2, summary.selectedGroupCount)
        assertEquals("2 groups selected", summary.selectedGroupCountLabel)
        assertEquals(
            listOf(
                PhotoDeletionGroupSummary(
                    groupLabel = "Group 1",
                    selectedCount = 1,
                    bytesToDelete = 1_000L,
                    keepDisplayName = "Receipt keep.png",
                    previewDisplayNames = listOf("Receipt delete.png"),
                    hiddenPreviewCount = 0,
                    isPriority = true,
                ),
                PhotoDeletionGroupSummary(
                    groupLabel = "Group 2",
                    selectedCount = 4,
                    bytesToDelete = 10_000L,
                    keepDisplayName = "AIRTEST-4.png",
                    previewDisplayNames = listOf("AIRTEST-3.png", "AIRTEST-2.png", "AIRTEST-1.png"),
                    hiddenPreviewCount = 1,
                    isPriority = false,
                ),
            ),
            summary.selectedGroups,
        )
    }

    @Test
    fun preservesOriginalGroupNumberWhenEarlierGroupsAreCleared() {
        val firstGroup = duplicateGroup(
            mediaItem("old-keep", 2_000L, "content://images/old-keep", "AIRSAFE-old-keep.png", dateTakenMillis = 500L),
            mediaItem("old-delete", 1_000L, "content://images/old-delete", "AIRSAFE-old-delete.png", dateTakenMillis = 400L),
            key = "old",
        )
        val secondGroup = duplicateGroup(
            mediaItem("test-keep", 2_000L, "content://images/test-keep", "AIRSAFE-4.png", dateTakenMillis = 300L),
            mediaItem("test-delete", 1_000L, "content://images/test-delete", "AIRSAFE-1.png", dateTakenMillis = 100L),
            key = "test",
        )
        val state = PhotoReviewSelectionState.fromGroups(
            groups = listOf(firstGroup, secondGroup),
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        ).deselectGroup(firstGroup.key)

        val summary = PhotoDeletionSummary.fromSimilarScreenshotSelection(
            groups = listOf(firstGroup, secondGroup),
            selectionState = state,
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertEquals("Group 2", summary.selectedGroups.single().groupLabel)
    }

    @Test
    fun matchesVisibleSimilarScreenshotGroupNumberWhenReviewOrderDiffersFromScanOrder() {
        val normalGroup = duplicateGroup(
            mediaItem("test-keep", 2_000L, "content://images/test-keep", "AIRCHECK-4.png", dateTakenMillis = 400L),
            mediaItem("test-delete", 1_000L, "content://images/test-delete", "AIRCHECK-1.png", dateTakenMillis = 100L),
            key = "test",
        )
        val priorityGroup = duplicateGroup(
            mediaItem("receipt-keep", 2_000L, "content://images/receipt-keep", "Receipt keep.png"),
            mediaItem("receipt-delete", 1_000L, "content://images/receipt-delete", "Receipt delete.png"),
            key = "receipt",
        )
        val scanOrderGroups = listOf(normalGroup, priorityGroup)
        val state = PhotoReviewSelectionState.fromGroups(
            groups = scanOrderGroups,
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        ).deselectGroup(priorityGroup.key)

        val summary = PhotoDeletionSummary.fromSimilarScreenshotSelection(
            groups = scanOrderGroups,
            selectionState = state,
            keepStrategy = PhotoReviewKeepStrategy.Newest,
        )

        assertEquals("Group 2", summary.selectedGroups.single().groupLabel)
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
        dateTakenMillis: Long = 0L,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = contentUri,
            width = width,
            height = height,
        )
    }
}
