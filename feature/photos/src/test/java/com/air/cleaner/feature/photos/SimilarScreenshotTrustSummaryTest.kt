package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarScreenshotTrustSummaryTest {
    @Test
    fun newestStrategyExplainsKeepChoiceAndRecoverableSpace() {
        val group = DuplicateGroup(
            key = "similar-screenshot:1",
            items = listOf(
                item("old", "Old screenshot.png", sizeBytes = 1_000_000L, dateTakenMillis = 1_000L),
                item("new", "New screenshot.png", sizeBytes = 2_000_000L, dateTakenMillis = 5_000L),
            ),
        )

        val summary = group.similarScreenshotTrustSummary(PhotoReviewKeepStrategy.Newest)

        assertEquals("Suggested keep: newest screenshot", summary.title)
        assertEquals(
            listOf(
                "Keeps New screenshot.png",
                "Why keep: newest capture in this group",
                "Can clean 1 photo | 1 MB",
                "Confidence: same screen size + visual fingerprint",
                "Range: captured 4 sec apart",
                "Risk: low; still review before deleting",
                "Review priority: normal",
            ),
            summary.lines,
        )
    }

    @Test
    fun farApartScreenshotsWarnUserToReviewCarefully() {
        val group = DuplicateGroup(
            key = "similar-screenshot:1",
            items = listOf(
                item("a", "Morning screenshot.png", dateTakenMillis = 1_000L),
                item("b", "Next day screenshot.png", dateTakenMillis = 90_001_000L),
            ),
        )

        val summary = group.similarScreenshotTrustSummary(PhotoReviewKeepStrategy.Newest)

        assertEquals(
            "Range: different sessions; review carefully",
            summary.lines[4],
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
            contentHash = null,
            mediaType = MediaType.Image,
            contentUri = "content://images/$id",
            width = 1440,
            height = 3120,
        )
    }
}
