# Similar Screenshot Fingerprint Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cache similar screenshot perceptual fingerprints so repeat scans avoid re-decoding unchanged screenshots.

**Architecture:** Keep grouping logic in `SimilarScreenshotScanner`, add a small `PerceptualFingerprintCache` boundary in `data:media`, and wire Android persistence through `SharedPreferences`. Cache keys include content uri, file size, date, width, and height so changed media invalidates automatically.

**Tech Stack:** Android Kotlin, SharedPreferences, existing JVM unit tests, Gradle Android modules.

---

### Task 1: Cache Key And Store Boundary

**Files:**
- Create: `data/media/src/main/java/com/air/cleaner/data/media/PerceptualFingerprintCache.kt`
- Create: `data/media/src/test/java/com/air/cleaner/data/media/PerceptualFingerprintCacheTest.kt`

- [x] **Step 1: Write failing tests**

Add tests that prove a cached hash is reused for the same media identity and recomputed when metadata changes.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :data:media:testDebugUnitTest --tests com.air.cleaner.data.media.PerceptualFingerprintCacheTest --no-daemon`

Expected: FAIL because `PerceptualFingerprintCache` does not exist.

- [x] **Step 3: Implement minimal cache boundary**

Add `PerceptualFingerprintCache`, an in-memory implementation for tests, and a stable cache-key builder.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :data:media:testDebugUnitTest --tests com.air.cleaner.data.media.PerceptualFingerprintCacheTest --no-daemon`

Expected: PASS.

### Task 2: Wire Cache Into Similar Screenshot Scan

**Files:**
- Modify: `data/media/src/main/java/com/air/cleaner/data/media/SimilarScreenshotScanner.kt`
- Modify: `data/media/src/main/java/com/air/cleaner/data/media/AndroidMediaStoreRepository.kt`
- Modify: `data/media/src/test/java/com/air/cleaner/data/media/SimilarScreenshotScannerTest.kt`

- [x] **Step 1: Add scanner test for unique screenshot read count**

Extend scanner tests so perceptual fingerprinting receives candidate metadata and still skips non-screenshots and unique dimensions.

- [x] **Step 2: Run scanner tests**

Run: `.\gradlew.bat :data:media:testDebugUnitTest --tests com.air.cleaner.data.media.SimilarScreenshotScannerTest --no-daemon`

Expected: PASS after signature updates.

- [x] **Step 3: Use cache in repository**

Build a cache key from each screenshot candidate and wrap `ImageContentFingerprinter.averageHash` with `cache.getOrPut`.

- [x] **Step 4: Verify data module and app**

Run: `.\gradlew.bat :data:media:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug --no-daemon`

Expected: PASS.

### Task 3: Device Validation

**Files:**
- No source files.

- [x] **Step 1: Install debug build**

Run: `.\gradlew.bat :app:installDebug --no-daemon`

Expected: installs on connected device.

- [x] **Step 2: Measure first and repeat Similar screenshots flow**

Open app, tap `Photos`, tap `Similar photos`, wait for result, then force-stop and repeat. Use UI dumps to capture visible result counts and wall-clock timing.

- [x] **Step 3: Commit**

Commit with the user-requested Chinese multi-paragraph format and include verification data.
