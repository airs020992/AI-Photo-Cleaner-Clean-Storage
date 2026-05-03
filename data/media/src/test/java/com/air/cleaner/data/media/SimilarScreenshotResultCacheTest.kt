package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarScreenshotResultCacheTest {
    @Test
    fun savesAndLoadsSimilarScreenshotGroups() {
        val cache = InMemorySimilarScreenshotResultCache()
        val groups = listOf(
            DuplicateGroup(
                key = "similar-screenshot:1",
                items = listOf(
                    item("1", "Screenshot_A.png", "content://media/images/1", 1_900_000L),
                    item("2", "Screenshot_B.png", "content://media/images/2", 1_800_000L),
                ),
            ),
        )

        cache.save(groups)
        val loaded = cache.load()

        assertEquals(groups, loaded)
        assertEquals("content://media/images/1", loaded.single().items.first().contentUri)
        assertEquals(1440, loaded.single().items.first().width)
        assertEquals(3120, loaded.single().items.first().height)
    }

    @Test
    fun returnsEmptyResultsForMalformedPayloads() {
        val cache = InMemorySimilarScreenshotResultCache()

        cache.saveRaw("not-a-valid-cache-payload")

        assertTrue(cache.load().isEmpty())
    }

    private fun item(
        id: String,
        displayName: String,
        contentUri: String,
        sizeBytes: Long,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = 1_000L,
            contentHash = null,
            mediaType = MediaType.Image,
            contentUri = contentUri,
            width = 1440,
            height = 3120,
        )
    }
}
