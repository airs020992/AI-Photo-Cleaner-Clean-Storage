# AI Photo Cleaner Internal Test Release Notes

Release date: 2026-05-06
Version: 0.1.0
Version code: 1
Package name: com.aiphotoclear.storagecleaner

## What To Test

AI Photo Cleaner helps users review storage-heavy media before deleting anything. This internal test build focuses on the first production wedge: safe review flows for large videos, similar photos, screenshots, and media access onboarding.

## Key User Flows

1. Open the app and grant photo/video access when prompted.
2. Confirm the Clean dashboard shows recoverable space and scanned item counts.
3. Open Large videos and verify the largest files are easy to identify before taking action.
4. Open Similar photos and verify repeated moments are grouped for review.
5. Open Photos and Videos tabs from the bottom navigation.
6. Open Settings and confirm diagnostics/privacy controls are reachable.

## Known Test Expectations

- Nothing should be deleted without explicit user confirmation.
- Android system deletion confirmation may appear for media deletion actions.
- Firebase Analytics initializes, but collection may remain disabled until the app privacy control allows it.
- The old package `com.air.cleaner` may still exist on local test devices; this build installs as a separate app under `com.aiphotoclear.storagecleaner`.

## Release Candidate

Upload this AAB for internal testing:

`app/build/outputs/bundle/release/app-release.aab`

