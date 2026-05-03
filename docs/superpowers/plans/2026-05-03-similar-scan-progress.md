# Similar Scan Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Similar screenshots loading page explain the current scan stage and how many screenshots are being checked.

**Architecture:** Keep scanning in `AIPhotoCleanerApp`. Add a small UI-facing scan status model for phase and known summary. The Similar loading screen reads that status and shows stage copy such as counting media, checking screenshot count, and finishing duplicate scan.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, existing JVM unit tests.

---

### Task 1: Scan Progress Copy

**Files:**
- Create: `app/src/test/java/com/air/cleaner/ui/MediaScanStatusTest.kt`
- Modify: `app/src/main/java/com/air/cleaner/ui/AIPhotoCleanerApp.kt`

- [x] **Step 1: Write failing tests**

Add tests for scan status copy:

```kotlin
@Test
fun similarLoadingCopyExplainsCountingBeforeSummaryExists() {
    val status = MediaScanStatus(MediaScanPhase.CountingLibrary)

    assertEquals("Step 1 of 3 | Counting media", status.similarLoadingStepLabel())
    assertEquals("Counting photos and screenshots so we can narrow the scan.", status.similarLoadingMessage())
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
    assertEquals("Comparing screenshots by visual fingerprint. Results appear here automatically.", status.similarLoadingMessage())
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.air.cleaner.ui.MediaScanStatusTest --no-daemon`

Expected: FAIL because the scan status model does not exist.

- [x] **Step 3: Implement minimal status model and copy functions**

Add `MediaScanPhase`, `MediaScanStatus`, `similarLoadingStepLabel()`, and `similarLoadingMessage()`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.air.cleaner.ui.MediaScanStatusTest --no-daemon`

Expected: PASS.

### Task 2: Wire Status Into Similar Loading UI

**Files:**
- Modify: `app/src/main/java/com/air/cleaner/ui/AIPhotoCleanerApp.kt`

- [x] **Step 1: Update scan state as each phase starts**

Set status to counting before `scanSummary()`, finding similar after summary returns, finding duplicates after similar returns, and complete after reconciliation.

- [x] **Step 2: Pass status into `SimilarScreenshotsLoadingScreen`**

Show the step label and stage message on the loading screen.

- [x] **Step 3: Verify**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug --no-daemon`

Expected: PASS.
