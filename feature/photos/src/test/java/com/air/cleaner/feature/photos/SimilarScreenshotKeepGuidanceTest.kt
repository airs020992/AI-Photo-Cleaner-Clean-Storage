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

    private fun item(
        id: String,
        displayName: String,
        sizeBytes: Long = 1_000_000L,
        dateTakenMillis: Long,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            contentHash = id,
            mediaType = MediaType.Image,
            contentUri = "content://images/$id",
            width = 1440,
            height = 3120,
        )
    }
}
