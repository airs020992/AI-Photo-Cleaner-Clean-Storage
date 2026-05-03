# Similar Screenshot Detection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a conservative V1 that groups near-identical screenshots for manual review without weakening exact duplicate safety.

**Architecture:** Keep exact duplicate detection unchanged. Add a separate similar screenshot scanner that only considers screenshot-like image paths/names, compares compact perceptual hashes inside dimension buckets, and exposes the result through the existing photo review flow with safer copy.

**Tech Stack:** Kotlin, Android MediaStore, Compose, JUnit, Android BitmapFactory.

---

### Task 1: Similar Screenshot Scanner

**Files:**
- Create: `data/media/src/main/java/com/air/cleaner/data/media/SimilarScreenshotScanner.kt`
- Test: `data/media/src/test/java/com/air/cleaner/data/media/SimilarScreenshotScannerTest.kt`

- [ ] Write failing tests for screenshot-only grouping, non-screenshot exclusion, and hamming threshold.
- [ ] Implement scanner with exact dimension buckets and max hash distance.
- [ ] Run `.\gradlew.bat :data:media:testDebugUnitTest --no-daemon`.

### Task 2: Perceptual Image Hash

**Files:**
- Modify: `data/media/src/main/java/com/air/cleaner/data/media/ImageContentFingerprinter.kt`
- Modify: `data/media/src/test/java/com/air/cleaner/data/media/ImageContentFingerprinterTest.kt`

- [ ] Write failing tests for stable average hash and hash distance.
- [ ] Implement 16x16 luma average hash plus Android bitmap decode entry point.
- [ ] Run `.\gradlew.bat :data:media:testDebugUnitTest --no-daemon`.

### Task 3: Wire Repository And UI

**Files:**
- Modify: `data/media/src/main/java/com/air/cleaner/data/media/MediaRepository.kt`
- Modify: `data/media/src/main/java/com/air/cleaner/data/media/AndroidMediaStoreRepository.kt`
- Modify: `app/src/main/java/com/air/cleaner/ui/AIPhotoCleanerApp.kt`
- Modify: `feature/photos/src/main/java/com/air/cleaner/feature/photos/PhotoReviewScreen.kt`

- [ ] Add `scanSimilarScreenshotGroups()` to the repository.
- [ ] Load similar groups during app scan.
- [ ] Make the Similar row open a review screen with “Similar screenshot” copy and no automatic delete.
- [ ] Run focused unit tests, compile, assemble, install, and launch smoke test.

### Task 4: Commit

- [ ] Commit with the user-requested multi-line Chinese format.
