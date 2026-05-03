package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarScreenshotMatchExplanationTest {
    @Test
    fun explainsSameSizeScreenshotsCapturedSecondsApart() {
        val group = DuplicateGroup(
            key = "similar-screenshot:1",
            items = listOf(
                item("1", dateTakenMillis = 1_000L),
                item("2", dateTakenMillis = 4_000L),
            ),
        )

        assertEquals(
            "Same screen size | captured 3 sec apart | tiny visual differences",
            group.similarScreenshotMatchExplanation(),
        )
    }

    @Test
    fun explainsSameSizeScreenshotsCapturedMinutesApart() {
        val group = DuplicateGroup(
            key = "similar-screenshot:1",
            items = listOf(
                item("1", dateTakenMillis = 1_000L),
                item("2", dateTakenMillis = 181_000L),
            ),
        )

        assertEquals(
            "Same screen size | captured 3 min apart | tiny visual differences",
            group.similarScreenshotMatchExplanation(),
        )
    }

    @Test
    fun hidesCaptureDistanceWhenTimestampsAreFarApart() {
        val group = DuplicateGroup(
            key = "similar-screenshot:1",
            items = listOf(
                item("1", dateTakenMillis = 1_000L),
                item("2", dateTakenMillis = 7_682_760_000L),
            ),
        )

        assertEquals(
            "Same screen size | tiny visual differences",
            group.similarScreenshotMatchExplanation(),
        )
    }

    private fun item(id: String, dateTakenMillis: Long): MediaItem {
        return MediaItem(
            id = id,
            displayName = "Screenshot_$id.png",
            sizeBytes = 1_000_000L,
            dateTakenMillis = dateTakenMillis,
            contentHash = null,
            mediaType = MediaType.Image,
            contentUri = "content://images/$id",
            width = 1440,
            height = 3120,
        )
    }
}
