package com.air.cleaner.ui

enum class AppTab(val label: String) {
    Clean("Clean"),
    Photos("Photos"),
    Videos("Videos"),
    Settings("Settings");

    companion object {
        val primaryTabs = listOf(Clean, Photos, Videos, Settings)
    }
}

sealed interface AppScreen {
    data class Tab(val tab: AppTab) : AppScreen
    data object DuplicatePhotoReview : AppScreen
    data object SimilarPhotoReview : AppScreen
    data object SimilarScreenshotReview : AppScreen
    data object LargeVideoReview : AppScreen
}

data class AppNavigationState(
    val selectedTab: AppTab = AppTab.Clean,
    val currentScreen: AppScreen = AppScreen.Tab(AppTab.Clean),
) {
    val shouldShowBottomTabs: Boolean = true

    fun selectTab(tab: AppTab): AppNavigationState {
        return copy(selectedTab = tab, currentScreen = AppScreen.Tab(tab))
    }

    fun openDuplicatePhotos(scanComplete: Boolean = true): AppNavigationState {
        if (!scanComplete) return this
        return copy(
            selectedTab = AppTab.Photos,
            currentScreen = AppScreen.DuplicatePhotoReview,
        )
    }

    fun openSimilarPhotos(scanComplete: Boolean = true): AppNavigationState {
        if (!scanComplete) return this
        return copy(
            selectedTab = AppTab.Photos,
            currentScreen = AppScreen.SimilarPhotoReview,
        )
    }

    fun openSimilarScreenshots(scanComplete: Boolean = true): AppNavigationState {
        return copy(
            selectedTab = AppTab.Photos,
            currentScreen = AppScreen.SimilarScreenshotReview,
        )
    }

    fun openLargeVideos(scanComplete: Boolean = true): AppNavigationState {
        if (!scanComplete) return this
        return copy(
            selectedTab = AppTab.Videos,
            currentScreen = AppScreen.LargeVideoReview,
        )
    }
}
