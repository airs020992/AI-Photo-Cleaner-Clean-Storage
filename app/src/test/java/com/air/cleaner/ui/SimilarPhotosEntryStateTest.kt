package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarPhotosEntryStateTest {
    @Test
    fun loadingBeforeGroupsShowsScanningMetric() {
        val state = similarPhotosEntryState(
            groups = null,
            reviewStatus = SimilarScreenshotReviewStatus.Loading,
            screenshotCount = null,
            formatBytes = { "$it bytes" },
        )

        assertEquals("Scanning", state.metric)
        assertEquals("Checking screenshots for near-identical captures", state.subtitle)
    }

    @Test
    fun loadingWithKnownScreenshotScopeShowsProgressScope() {
        val state = similarPhotosEntryState(
            groups = null,
            reviewStatus = SimilarScreenshotReviewStatus.Loading,
            screenshotCount = 37,
            formatBytes = { "$it bytes" },
        )

        assertEquals("37 screenshots", state.metric)
        assertEquals("Checking 37 screenshots for near-identical captures", state.subtitle)
    }

    @Test
    fun freshGroupsShowRecoverableSpaceAndReadyCopy() {
        val state = similarPhotosEntryState(
            groups = listOf(group("ready")),
            reviewStatus = SimilarScreenshotReviewStatus.Fresh,
            screenshotCount = 2,
            formatBytes = { "$it bytes" },
        )

        assertEquals("1000 bytes", state.metric)
        assertEquals("Near-identical screenshots ready for review", state.subtitle)
    }

    @Test
    fun freshEmptyGroupsExplainSafeThreshold() {
        val state = similarPhotosEntryState(
            groups = emptyList(),
            reviewStatus = SimilarScreenshotReviewStatus.Fresh,
            screenshotCount = 12,
            formatBytes = { "$it bytes" },
        )

        assertEquals("0 found", state.metric)
        assertEquals("No safe matches yet. Try 2-3 screenshots of the same screen.", state.subtitle)
    }

    @Test
    fun freshEmptyGroupsExplainZeroScreenshotsInScope() {
        val state = similarPhotosEntryState(
            groups = emptyList(),
            reviewStatus = SimilarScreenshotReviewStatus.Fresh,
            screenshotCount = 0,
            formatBytes = { "$it bytes" },
        )

        assertEquals("0 screenshots", state.metric)
        assertEquals("No screenshots in access scope. Capture or allow photos, then rescan.", state.subtitle)
    }

    private fun group(key: String): DuplicateGroup {
        return DuplicateGroup(
            key = key,
            items = listOf(
                MediaItem(
                    id = "$key-a",
                    displayName = "$key-a.jpg",
                    sizeBytes = 2_000L,
                    dateTakenMillis = 1_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-a",
                ),
                MediaItem(
                    id = "$key-b",
                    displayName = "$key-b.jpg",
                    sizeBytes = 1_000L,
                    dateTakenMillis = 2_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-b",
                ),
            ),
        )
    }
}
