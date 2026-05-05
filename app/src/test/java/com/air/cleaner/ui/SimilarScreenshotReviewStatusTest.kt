package com.air.cleaner.ui

import com.air.cleaner.data.media.MediaScanSummary
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
    fun cachedGroupsShowDisabledBackgroundRefreshAction() {
        val state = SimilarScreenshotReviewStatus.CachedRefreshing.reviewActionState(hasGroups = true)

        assertEquals("Refreshing results", state?.title)
        assertEquals(
            "Saved groups stay visible while we check your latest screenshots.",
            state?.message,
        )
        assertEquals("Refreshing", state?.actionLabel)
        assertEquals(false, state?.actionEnabled)
    }

    @Test
    fun freshGroupsOfferManualRescanAction() {
        val state = SimilarScreenshotReviewStatus.Fresh.reviewActionState(hasGroups = true)

        assertEquals("Took new screenshots?", state?.title)
        assertEquals(
            "Scan again before deleting to include the latest captures.",
            state?.message,
        )
        assertEquals("Scan again", state?.actionLabel)
        assertEquals(true, state?.actionEnabled)
    }

    @Test
    fun emptyResultsUseEmptyStateRescanInsteadOfReviewAction() {
        val state = SimilarScreenshotReviewStatus.Fresh.reviewActionState(hasGroups = false)

        assertNull(state)
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
    fun freshEmptyResultsExplainCheckedScreenshotScope() {
        val status = SimilarScreenshotReviewStatus.Fresh

        assertEquals(
            "We checked 12 screenshots for near-identical layouts and tiny visual changes. No safe review groups passed the confidence threshold yet.\n\nWhat to try: take 2-3 screenshots of the same screen, then tap Rescan photos.",
            status.emptyMessage(scanStatusWithScreenshots(12)),
        )
    }

    @Test
    fun freshEmptyResultsExplainWhenNoScreenshotsAreInScope() {
        val status = SimilarScreenshotReviewStatus.Fresh

        assertEquals(
            "No screenshots to compare",
            status.emptyTitle(scanStatusWithScreenshots(0)),
        )
        assertEquals(
            "We found 0 screenshots in the current photo access scope.\n\nWhat to try: take a few screenshots of the same screen, or allow full photo access, then tap Rescan photos.",
            status.emptyMessage(scanStatusWithScreenshots(0)),
        )
    }

    @Test
    fun filteredCacheEmptyKeepsStaleResultExplanationWithCurrentScope() {
        val status = SimilarScreenshotReviewStatus.FilteredCacheEmpty

        assertEquals(
            "Previous matches included photos that no longer exist. We removed stale candidates and are checking your library again.\n\nCurrent scan: 7 screenshots in scope.",
            status.emptyMessage(scanStatusWithScreenshots(7)),
        )
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

    private fun scanStatusWithScreenshots(screenshotCount: Int): MediaScanStatus {
        return MediaScanStatus(
            phase = MediaScanPhase.Complete,
            summary = MediaScanSummary(
                imageCount = screenshotCount,
                videoCount = 0,
                imageBytes = 0L,
                videoBytes = 0L,
                screenshotCount = screenshotCount,
                screenshotBytes = 0L,
            ),
        )
    }
}
