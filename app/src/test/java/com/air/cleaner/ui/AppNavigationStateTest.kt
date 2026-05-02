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
}
