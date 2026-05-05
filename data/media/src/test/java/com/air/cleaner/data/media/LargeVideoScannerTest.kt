package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class LargeVideoScannerTest {
    @Test
    fun returnsLargestVideosInDescendingSizeOrder() {
        val videos = LargeVideoScanner(topCount = 3).findLargeVideos(
            listOf(
                mediaItem(id = "small", sizeBytes = 80_000_000L),
                mediaItem(id = "largest", sizeBytes = 2_400_000_000L),
                mediaItem(id = "image", sizeBytes = 9_000_000_000L, mediaType = MediaType.Image),
                mediaItem(id = "medium", sizeBytes = 700_000_000L),
                mediaItem(id = "large", sizeBytes = 1_200_000_000L),
            ),
        )

        assertEquals(listOf("largest", "large", "medium"), videos.map { it.id })
        assertEquals(4_300_000_000L, videos.sumOf { it.sizeBytes })
    }

    @Test
    fun returnsEmptyListWhenNoVideosAreVisible() {
        val videos = LargeVideoScanner().findLargeVideos(
            listOf(mediaItem(id = "image", sizeBytes = 9_000_000_000L, mediaType = MediaType.Image)),
        )

        assertEquals(emptyList<MediaItem>(), videos)
    }

    private fun mediaItem(
        id: String,
        sizeBytes: Long,
        mediaType: MediaType = MediaType.Video,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = "$id.mp4",
            sizeBytes = sizeBytes,
            dateTakenMillis = 1_700_000_000_000L,
            contentHash = null,
            mediaType = mediaType,
        )
    }
}
