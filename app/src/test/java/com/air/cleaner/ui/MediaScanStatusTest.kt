package com.air.cleaner.ui

import com.air.cleaner.data.media.MediaScanSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaScanStatusTest {
    @Test
    fun similarLoadingCopyExplainsCountingBeforeSummaryExists() {
        val status = MediaScanStatus(MediaScanPhase.CountingLibrary)

        assertEquals("Step 1 of 3 | Counting media", status.similarLoadingStepLabel())
        assertEquals(
            "Counting photos and screenshots so we can narrow the scan.",
            status.similarLoadingMessage(),
        )
    }

    @Test
    fun similarLoadingCopyIncludesScreenshotCountAfterSummaryExists() {
        val status = MediaScanStatus(
            phase = MediaScanPhase.FindingSimilarScreenshots,
            summary = MediaScanSummary(
                imageCount = 200,
                videoCount = 10,
                imageBytes = 100L,
                videoBytes = 20L,
                screenshotCount = 37,
                screenshotBytes = 30L,
            ),
        )

        assertEquals("Step 2 of 3 | Checking 37 screenshots", status.similarLoadingStepLabel())
        assertEquals(
            "Comparing screenshots by visual fingerprint. Results appear here automatically.",
            status.similarLoadingMessage(),
        )
    }
}
