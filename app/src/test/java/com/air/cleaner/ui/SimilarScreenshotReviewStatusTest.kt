package com.air.cleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimilarScreenshotReviewStatusTest {
    @Test
    fun cachedGroupsShowRefreshingNotice() {
        val status = SimilarScreenshotReviewStatus.CachedRefreshing

        assertEquals("Showing saved results", status.noticeTitle())
        assertEquals(
            "Refreshing in the background. Review stays available while we check for library changes.",
            status.noticeMessage(),
        )
    }

    @Test
    fun freshGroupsDoNotShowNotice() {
        val status = SimilarScreenshotReviewStatus.Fresh

        assertNull(status.noticeTitle())
        assertNull(status.noticeMessage())
    }

    @Test
    fun freshEmptyResultsInviteManualRescan() {
        val status = SimilarScreenshotReviewStatus.Fresh

        assertEquals("No similar screenshots found", status.emptyTitle())
        assertEquals(
            "We checked screenshots for near-identical layouts and tiny visual changes. Nothing reached the safe review threshold yet. Try taking 2-3 screenshots of the same screen, then rescan.",
            status.emptyMessage(),
        )
        assertEquals("Rescan photos", status.emptyActionLabel())
    }

    @Test
    fun filteredCacheExplainsWhyResultsDisappeared() {
        val status = SimilarScreenshotReviewStatus.FilteredCacheEmpty

        assertEquals("Saved results were updated", status.emptyTitle())
        assertEquals(
            "Previous matches included photos that no longer exist. We removed stale candidates and are checking your library again.",
            status.emptyMessage(),
        )
        assertEquals("Rescan photos", status.emptyActionLabel())
    }
}
