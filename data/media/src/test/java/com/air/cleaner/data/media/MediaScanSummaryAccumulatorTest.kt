package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaScanSummaryAccumulatorTest {
    @Test
    fun accumulatesImageVideoAndScreenshotBuckets() {
        val accumulator = MediaScanSummaryAccumulator()

        accumulator.add(
            item = mediaItem("1", "IMG_1.jpg", 1_000L, MediaType.Image),
            relativePath = "DCIM/Camera/",
        )
        accumulator.add(
            item = mediaItem("2", "Screenshot_2.png", 2_000L, MediaType.Image),
            relativePath = "Pictures/Screenshots/",
        )
        accumulator.add(
            item = mediaItem("3", "VID_3.mp4", 5_000L, MediaType.Video),
            relativePath = "Movies/",
        )

        val summary = accumulator.summary()

        assertEquals(2, summary.imageCount)
        assertEquals(1, summary.videoCount)
        assertEquals(3_000L, summary.imageBytes)
        assertEquals(5_000L, summary.videoBytes)
        assertEquals(1, summary.screenshotCount)
        assertEquals(2_000L, summary.screenshotBytes)
        assertEquals(3, summary.totalCount)
        assertEquals(8_000L, summary.totalBytes)
    }

    private fun mediaItem(
        id: String,
        displayName: String,
        sizeBytes: Long,
        mediaType: MediaType,
    ): MediaItem {
        return MediaItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = 0L,
            contentHash = null,
            mediaType = mediaType,
        )
    }
}
