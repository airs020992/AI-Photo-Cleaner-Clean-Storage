package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeVideosEntryStateTest {
    @Test
    fun summarizesTopVideosForVideosTab() {
        val state = largeVideosEntryState(
            videos = listOf(
                video("Travel.mov", 2_400_000_000L, 185_000L),
                video("Screen recording.mp4", 720_000_000L, 75_000L),
            ),
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals("2 videos ready to review", state.subtitle)
        assertEquals("3120 MB", state.totalBytesLabel)
        assertTrue(state.actionEnabled)
        assertEquals(
            "Compression estimate first. Android asks again before deletion.",
            state.statusLabel,
        )
        assertEquals(
            listOf(
                LargeVideoRowState("Travel.mov", "3:05 | 2400 MB", "2400 MB"),
                LargeVideoRowState("Screen recording.mp4", "1:15 | 720 MB", "720 MB"),
            ),
            state.rows,
        )
    }

    @Test
    fun reportsEmptyStateWhenScanHasNoVideos() {
        val state = largeVideosEntryState(videos = emptyList(), formatBytes = { "$it B" })

        assertEquals("No large videos found", state.subtitle)
        assertEquals("0 B", state.totalBytesLabel)
        assertFalse(state.actionEnabled)
        assertEquals(
            "No action needed. New videos will appear after the next scan.",
            state.statusLabel,
        )
        assertEquals(emptyList<LargeVideoRowState>(), state.rows)
    }

    @Test
    fun reportsScanningStateBeforeLargeVideoScanFinishes() {
        val state = largeVideosEntryState(
            videos = null,
            fallbackTotalBytesLabel = "14 GB",
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals("Scanning videos by size", state.subtitle)
        assertEquals("14 GB", state.totalBytesLabel)
        assertFalse(state.actionEnabled)
        assertEquals(
            "Originals stay untouched until you confirm.",
            state.statusLabel,
        )
        assertEquals(emptyList<LargeVideoRowState>(), state.rows)
    }

    @Test
    fun labelsCappedTopTwentyResultSoPostDeleteRefillIsUnderstandable() {
        val videos = (1..20).map { index ->
            video(
                displayName = "Clip-$index.mp4",
                sizeBytes = index * 1_000_000L,
                durationMillis = index * 1_000L,
            )
        }

        val state = largeVideosEntryState(
            videos = videos,
            formatBytes = { bytes -> "${bytes / 1_000_000} MB" },
        )

        assertEquals("Top 20 largest videos ready to review", state.subtitle)
        assertTrue(state.actionEnabled)
    }

    private fun video(
        displayName: String,
        sizeBytes: Long,
        durationMillis: Long,
    ): MediaItem {
        return MediaItem(
            id = displayName,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateTakenMillis = 1_700_000_000_000L,
            contentHash = null,
            mediaType = MediaType.Video,
            durationMillis = durationMillis,
        )
    }
}
