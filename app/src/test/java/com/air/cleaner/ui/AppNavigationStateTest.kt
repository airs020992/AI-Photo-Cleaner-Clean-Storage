package com.air.cleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationStateTest {
    @Test
    fun bottomNavigationUsesFourPrimaryTabs() {
        assertEquals(
            listOf(AppTab.Clean, AppTab.Photos, AppTab.Videos, AppTab.Settings),
            AppTab.primaryTabs,
        )
    }

    @Test
    fun appStartsOnCleanDashboard() {
        val state = AppNavigationState()

        assertEquals(AppTab.Clean, state.selectedTab)
        assertEquals(AppScreen.Tab(AppTab.Clean), state.currentScreen)
    }

    @Test
    fun duplicatePhotoActionOpensReviewFlowUnderPhotos() {
        val state = AppNavigationState()

        val next = state.openDuplicatePhotos()

        assertEquals(AppTab.Photos, next.selectedTab)
        assertEquals(AppScreen.DuplicatePhotoReview, next.currentScreen)
        assertTrue(next.shouldShowBottomTabs)
    }

    @Test
    fun similarScreenshotActionOpensReviewWhileScanIsStillRunning() {
        val state = AppNavigationState(selectedTab = AppTab.Photos, currentScreen = AppScreen.Tab(AppTab.Photos))

        val next = state.openSimilarScreenshots(scanComplete = false)

        assertEquals(AppTab.Photos, next.selectedTab)
        assertEquals(AppScreen.SimilarScreenshotReview, next.currentScreen)
    }

    @Test
    fun similarPhotoActionOpensReviewAfterScanCompletes() {
        val state = AppNavigationState()

        val next = state.openSimilarPhotos(scanComplete = true)

        assertEquals(AppTab.Photos, next.selectedTab)
        assertEquals(AppScreen.SimilarPhotoReview, next.currentScreen)
    }

    @Test
    fun similarPhotoActionWaitsForScanCompletion() {
        val state = AppNavigationState()

        val next = state.openSimilarPhotos(scanComplete = false)

        assertEquals(state, next)
    }

    @Test
    fun largeVideosActionOpensReviewAfterScanCompletes() {
        val state = AppNavigationState()

        val next = state.openLargeVideos(scanComplete = true)

        assertEquals(AppTab.Videos, next.selectedTab)
        assertEquals(AppScreen.LargeVideoReview, next.currentScreen)
    }

    @Test
    fun largeVideosActionWaitsForScanCompletion() {
        val state = AppNavigationState()

        val next = state.openLargeVideos(scanComplete = false)

        assertEquals(state, next)
    }

    @Test
    fun similarScreenshotActionOpensReviewAfterScanCompletes() {
        val state = AppNavigationState()

        val next = state.openSimilarScreenshots(scanComplete = true)

        assertEquals(AppTab.Photos, next.selectedTab)
        assertEquals(AppScreen.SimilarScreenshotReview, next.currentScreen)
    }
}
