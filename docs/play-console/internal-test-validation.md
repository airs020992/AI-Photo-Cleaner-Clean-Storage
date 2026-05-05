# Internal Test Validation Plan

Last updated: 2026-05-06

This plan verifies the Play-distributed build after the AAB is uploaded to Internal testing.

## Device Baseline

- Test device used locally: `1A071FDEE00538`
- Package: `com.aiphotoclear.storagecleaner`
- Local debug smoke result before Play upload: install success, launch success, no `FATAL EXCEPTION` in recent logcat.

## Play Install Path

1. Add tester Gmail accounts in Play Console.
2. Roll out `0.1.0 (1)` to Internal testing.
3. Open the internal testing opt-in link on the test device.
4. Install AI Photo Cleaner from Google Play.
5. Confirm Android package is `com.aiphotoclear.storagecleaner`.

## Smoke Tests

### Startup

- Launch from launcher.
- Expected: App opens to onboarding or Clean dashboard.
- Evidence to capture:
  - `adb shell pidof com.aiphotoclear.storagecleaner`
  - `adb shell dumpsys activity activities | findstr com.aiphotoclear.storagecleaner`
  - `adb logcat -d -t 1000 | findstr "FATAL EXCEPTION AndroidRuntime FirebaseApp FirebaseInitProvider"`

### Permissions

- Grant full media access for first internal test pass.
- Expected: Settings diagnostics says media access is full library.

### Large Videos

- Open bottom tab `Videos`.
- Tap `Large videos`.
- Expected: Top largest videos appear when videos exist.
- Expected: Compression estimate is visible before destructive action.
- Expected: Original deletion remains gated until output is verified and Android confirms.

### Similar Photos

- Open bottom tab `Photos`.
- Tap `Similar photos`.
- Expected: Progress/scan state appears quickly, not a dead screen.
- Expected: Similar groups appear when staged samples exist.
- Expected: Review page explains why files match and keeps one default candidate.

### Settings / Analytics

- Open bottom tab `Settings`.
- Check Product analytics toggle and diagnostics.
- Expected: If analytics is off, diagnostics still record local events and Firebase delivery says paused.
- Expected: If analytics is on, Firebase DebugView should receive events after DebugView setup.

## Acceptance Gate

Internal testing is acceptable when all are true:

- Play-distributed build installs and launches.
- No startup crash in logcat.
- Large videos flow reaches review screen on a device with videos.
- Similar photos flow shows responsive scan feedback.
- Privacy policy URL opens from Play Console.
- Store assets render correctly in Play listing preview.
