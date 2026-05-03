# Similar Scan Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make tapping Similar photos feel responsive while screenshot similarity scanning is still running.

**Architecture:** Keep scan ownership in `AIPhotoCleanerApp`. Navigation can open the Similar screenshots review before results exist; that screen renders a loading state until `similarScreenshotGroups` becomes non-null, then swaps to the existing review or empty state.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, existing app unit tests.

---

### Task 1: Navigation Feedback

**Files:**
- Modify: `app/src/test/java/com/air/cleaner/ui/AppNavigationStateTest.kt`
- Modify: `app/src/main/java/com/air/cleaner/ui/AppNavigationState.kt`

- [x] **Step 1: Write the failing test**

Change the pending Similar test to expect review navigation even when scan is incomplete:

```kotlin
@Test
fun similarScreenshotActionOpensReviewWhileScanIsStillRunning() {
    val state = AppNavigationState(selectedTab = AppTab.Photos, currentScreen = AppScreen.Tab(AppTab.Photos))

    val next = state.openSimilarScreenshots(scanComplete = false)

    assertEquals(AppTab.Photos, next.selectedTab)
    assertEquals(AppScreen.SimilarScreenshotReview, next.currentScreen)
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.air.cleaner.ui.AppNavigationStateTest --no-daemon`

Expected: FAIL because `openSimilarScreenshots(scanComplete = false)` still returns the existing state.

- [x] **Step 3: Write minimal implementation**

Remove the incomplete-scan guard from `openSimilarScreenshots`; keep it for duplicate photos.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.air.cleaner.ui.AppNavigationStateTest --no-daemon`

Expected: PASS.

### Task 2: Review Loading State

**Files:**
- Modify: `app/src/main/java/com/air/cleaner/ui/AIPhotoCleanerApp.kt`

- [x] **Step 1: Implement loading review UI**

When `AppScreen.SimilarScreenshotReview` is active and `similarScreenshotGroups == null`, render a Material loading state with clear copy instead of passing `orEmpty()` into `PhotoReviewScreen`.

- [x] **Step 2: Make Similar row clickable while scanning**

In `PhotosTabScreen` and `CleanTabScreen`, remove the `similarScreenshotGroups != null` click gate for Similar only.

- [x] **Step 3: Verify**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug --no-daemon`

Expected: PASS.
