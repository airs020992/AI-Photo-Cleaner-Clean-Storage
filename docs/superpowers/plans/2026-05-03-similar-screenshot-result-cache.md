# Similar Screenshot Result Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show cached Similar screenshots results immediately while the app refreshes the scan in the background.

**Architecture:** Add a `SimilarScreenshotResultCache` boundary in `data:media` with in-memory and SharedPreferences implementations. `AndroidMediaStoreRepository` exposes cached result load/save through `MediaRepository`; `AIPhotoCleanerApp` hydrates `similarScreenshotGroups` from cache before running the full scan, then saves the fresh scan result.

**Tech Stack:** Android Kotlin, SharedPreferences, existing DuplicateGroup domain model, JVM unit tests, Gradle Android modules.

---

### Task 1: Result Cache Serialization

**Files:**
- Create: `data/media/src/main/java/com/air/cleaner/data/media/SimilarScreenshotResultCache.kt`
- Create: `data/media/src/test/java/com/air/cleaner/data/media/SimilarScreenshotResultCacheTest.kt`

- [x] **Step 1: Write failing tests**

Add tests that save a `DuplicateGroup` containing screenshot `MediaItem` values, load it back, and return empty results for malformed payloads.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :data:media:testDebugUnitTest --tests com.air.cleaner.data.media.SimilarScreenshotResultCacheTest --no-daemon`

Expected: FAIL because the cache type does not exist.

- [x] **Step 3: Implement minimal cache**

Add `SimilarScreenshotResultCache`, `InMemorySimilarScreenshotResultCache`, `SharedPreferencesSimilarScreenshotResultCache`, and a small versioned serializer.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :data:media:testDebugUnitTest --tests com.air.cleaner.data.media.SimilarScreenshotResultCacheTest --no-daemon`

Expected: PASS.

### Task 2: Repository And UI Hydration

**Files:**
- Modify: `data/media/src/main/java/com/air/cleaner/data/media/MediaRepository.kt`
- Modify: `data/media/src/main/java/com/air/cleaner/data/media/AndroidMediaStoreRepository.kt`
- Modify: `app/src/main/java/com/air/cleaner/ui/AIPhotoCleanerApp.kt`

- [x] **Step 1: Add repository methods**

Expose `cachedSimilarScreenshotGroups()` and `saveSimilarScreenshotGroups(groups)`.

- [x] **Step 2: Hydrate UI from cache**

In `AIPhotoCleanerApp`, load cached groups before `scanSummary()`. After fresh `scanSimilarScreenshotGroups()`, save the fresh result.

- [x] **Step 3: Verify**

Run: `.\gradlew.bat :data:media:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug --no-daemon`

Expected: PASS.

### Task 3: Device Timing Validation

**Files:**
- No source files.

- [x] **Step 1: Install debug build**

Run: `.\gradlew.bat :app:installDebug --no-daemon`

Expected: installs on connected Pixel 6 Pro.

- [x] **Step 2: Measure repeat open**

Force-stop app twice and measure time from tapping `Similar photos` to cached result text.

- [x] **Step 3: Commit**

Commit with the user-requested Chinese multi-paragraph format and include timing data.
