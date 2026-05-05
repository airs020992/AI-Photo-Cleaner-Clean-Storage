package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarPhotoMatchExplanationTest {
    @Test
    fun explainsTwoPhotosCapturedSecondsApartWithVeryCloseSizes() {
        val group = DuplicateGroup(
            key = "similar-photo:1",
            items = listOf(
                item("1", sizeBytes = 1_000_000L, dateTakenMillis = 1_000L),
                item("2", sizeBytes = 1_050_000L, dateTakenMillis = 31_000L),
            ),
        )

        assertEquals(
            "2 photos captured within 30 sec | very close file sizes | keep one best shot",
            group.similarPhotoMatchExplanation(),
        )
    }

    @Test
    fun explainsBurstWithCloseSizesCapturedMinutesApart() {
        val group = DuplicateGroup(
            key = "similar-photo:2",
            items = listOf(
                item("1", sizeBytes = 1_000_000L, dateTakenMillis = 1_000L),
                item("2", sizeBytes = 1_160_000L, dateTakenMillis = 61_000L),
                item("3", sizeBytes = 1_210_000L, dateTakenMillis = 121_000L),
            ),
        )

        assertEquals(
            "3 photos captured within 2 min | close file sizes | keep one best shot",
            group.similarPhotoMatchExplanation(),
        )
    }

    private fun item(id: String, sizeBytes: Long, dateTakenMillis: Long): MediaItem {
        return MediaItem(
            id = id,
            displayName = "IMG_$id.jpg",
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            contentHash = null,
            mediaType = MediaType.Image,
            contentUri = "content://images/$id",
            width = 3024,
            height = 4032,
        )
    }
}
