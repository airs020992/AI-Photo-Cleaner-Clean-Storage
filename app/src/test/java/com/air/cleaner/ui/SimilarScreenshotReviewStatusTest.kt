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
}
