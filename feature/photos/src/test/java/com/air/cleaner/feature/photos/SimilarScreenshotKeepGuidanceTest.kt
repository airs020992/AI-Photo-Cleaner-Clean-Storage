package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarScreenshotKeepGuidanceTest {
    @Test
    fun newestStrategyExplainsThatNewestCaptureIsProtected() {
        val group = DuplicateGroup(
            key = "similar-screenshot:1",
            items = listOf(
                item("old", "Screenshot old.png", sizeBytes = 2_000_000L, dateTakenMillis = 1_000L),
                item("new", "Screenshot new.png", sizeBytes = 1_000_000L, dateTakenMillis = 5_000L),
            ),
        )

        val guidance = group.similarScreenshotKeepGuidance(PhotoReviewKeepStrategy.Newest)

        assertEquals("Why keep: newest capture in this group", guidance.keepReasonLine)
        assertEquals("Risk: low; still review before deleting", guidance.riskLine)
    }

    @Test
    fun sensitiveScreenshotNamesWarnBeforeDeleting() {
        val group = DuplicateGroup(
            key = "similar-screenshot:receipt",
            items = listOf(
                item("receipt-1", "Receipt checkout confirmation.png", dateTakenMillis = 1_000L),
                item("receipt-2", "Receipt checkout confirmation copy.png", dateTakenMillis = 2_000L),
            ),
        )

        val guidance = group.similarScreenshotKeepGuidance(PhotoReviewKeepStrategy.Recommended)

        assertEquals("Why keep: highest-quality candidate", guidance.keepReasonLine)
        assertEquals(
            "Risk: may contain private or transaction details; review before deleting",
            guidance.riskLine,
        )
    }

    @Test
    fun farApartScreenshotsRequireHighPriorityReview() {
        val group = DuplicateGroup(
            key = "similar-screenshot:far-apart",
            items = listOf(
                item("morning", "Screenshot morning.png", dateTakenMillis = 1_000L),
                item("next-day", "Screenshot next day.png", dateTakenMillis = 90_001_000L),
            ),
        )

        val guidance = group.similarScreenshotKeepGuidance(PhotoReviewKeepStrategy.Newest)

        assertEquals(
            "Risk: captured in different sessions; review before deleting",
            guidance.riskLine,
        )
        assertEquals("Review priority: high", guidance.reviewPriorityLine)
    }

    @Test
    fun mixedDimensionsRequireMediumPriorityReview() {
        val group = DuplicateGroup(
            key = "similar-screenshot:mixed-dimensions",
            items = listOf(
                item("portrait", "Screenshot portrait.png", dateTakenMillis = 1_000L, width = 1440, height = 3120),
                item("crop", "Screenshot crop.png", dateTakenMillis = 2_000L, width = 1080, height = 2400),
            ),
        )

        val guidance = group.similarScreenshotKeepGuidance(PhotoReviewKeepStrategy.Newest)

        assertEquals(
            "Risk: mixed dimensions; compare before deleting",
            guidance.riskLine,
        )
        assertEquals("Review priority: medium", guidance.reviewPriorityLine)
    }

    private fun item(
        id: String,
        displayName: String,
        sizeBytes: Long = 1_000_000L,
        dateTakenMillis: Long,
        width: Int? = 1440,
        height: Int? = 3120,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = "content://images/$id",
            width = width,
            height = height,
        )
    }
}
