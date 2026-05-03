package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarScreenshotReviewWorkflowTest {
    @Test
    fun ordersHighPriorityGroupsBeforeMediumAndNormalGroups() {
        val groups = listOf(
            normalGroup(),
            mixedDimensionsGroup(),
            sensitiveGroup(),
        )

        val workflow = groups.toSimilarScreenshotReviewWorkflow(
            keepStrategy = PhotoReviewKeepStrategy.Newest,
            filter = SimilarScreenshotReviewFilter.All,
        )

        assertEquals(listOf("sensitive", "mixed", "normal"), workflow.groups.map { it.key })
        assertEquals("3 groups", workflow.totalGroupsLabel)
        assertEquals("2 need review", workflow.needsReviewGroupsLabel)
        assertEquals("All", workflow.activeFilterLabel)
    }

    @Test
    fun filtersToGroupsNeedingReviewOnly() {
        val groups = listOf(
            normalGroup(),
            mixedDimensionsGroup(),
            sensitiveGroup(),
        )

        val workflow = groups.toSimilarScreenshotReviewWorkflow(
            keepStrategy = PhotoReviewKeepStrategy.Newest,
            filter = SimilarScreenshotReviewFilter.NeedsReview,
        )

        assertEquals(listOf("sensitive", "mixed"), workflow.groups.map { it.key })
        assertEquals("Needs review", workflow.activeFilterLabel)
    }

    private fun sensitiveGroup(): DuplicateGroup {
        return DuplicateGroup(
            key = "sensitive",
            items = listOf(
                item("receipt-1", "Receipt checkout.png", dateTakenMillis = 1_000L),
                item("receipt-2", "Receipt checkout copy.png", dateTakenMillis = 2_000L),
            ),
        )
    }

    private fun mixedDimensionsGroup(): DuplicateGroup {
        return DuplicateGroup(
            key = "mixed",
            items = listOf(
                item("mixed-1", "Screenshot portrait.png", dateTakenMillis = 3_000L, width = 1440, height = 3120),
                item("mixed-2", "Screenshot crop.png", dateTakenMillis = 4_000L, width = 1080, height = 2400),
            ),
        )
    }

    private fun normalGroup(): DuplicateGroup {
        return DuplicateGroup(
            key = "normal",
            items = listOf(
                item("normal-1", "Screenshot dashboard.png", dateTakenMillis = 5_000L),
                item("normal-2", "Screenshot dashboard copy.png", dateTakenMillis = 6_000L),
            ),
        )
    }

    private fun item(
        id: String,
        displayName: String,
        dateTakenMillis: Long,
        width: Int? = 1440,
        height: Int? = 3120,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = 1_000_000L,
            dateTakenMillis = dateTakenMillis,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = "content://images/$id",
            width = width,
            height = height,
        )
    }
}
