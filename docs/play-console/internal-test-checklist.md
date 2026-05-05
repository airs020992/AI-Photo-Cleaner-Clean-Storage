# Play Console Internal Test Checklist

Last updated: 2026-05-06
App: AI Photo Cleaner
Package: com.aiphotoclear.storagecleaner

## Build Candidate

- AAB: `app/build/outputs/bundle/release/app-release.aab`
- Upload manifest: `docs/play-console/release-upload-manifest.md`
- Version name: `0.1.0`
- Version code: `1`
- Package name: `com.aiphotoclear.storagecleaner`
- Firebase app id: `1:186772633663:android:a4d59cc0e70b10c06a608a`
- Upload key SHA1: `42:21:AC:55:C2:58:02:62:33:93:A8:BB:33:CD:5A:7C:A6:59:1D:D4`
- Upload key SHA256: `60:00:44:58:A1:67:A0:4D:50:89:92:0B:C5:B9:DC:4A:0F:D2:BC:7F:A5:2B:23:A4:FE:9F:A6:60:E9:4D:2A:11`

## Before Upload

- Back up `app/upload-keystore.jks`.
- Back up `local-signing.properties`.
- Confirm these files are not committed.
- Confirm the AAB is signed.
- Confirm Play Console package is `com.aiphotoclear.storagecleaner`.

## Play Console Steps

1. Open Testing > Internal testing.
2. Create a new release.
3. Upload `app-release.aab` and verify its SHA256 against `docs/play-console/release-upload-manifest.md`.
4. Add release notes from `docs/play-console/internal-test-release-notes.md`.
5. Add internal testers.
6. Complete required app content sections using `docs/play-console/play-console-form-answers.md`.
7. Roll out to internal testing.
8. Validate the Play-distributed build using `docs/play-console/internal-test-validation.md`.

## Required Store/Compliance Assets

- Privacy policy URL: `https://airs020992.github.io/AI-Photo-Cleaner-Clean-Storage/privacy-policy.html`
- App icon.
- Feature graphic.
- Phone screenshots.
- Short description.
- Full description.
- Data safety form.
- Content rating questionnaire.
- Target audience and content settings.
- Ads declaration.
- Play Console form answer source: `docs/play-console/play-console-form-answers.md`

## Internal Test Acceptance Criteria

- App installs from Play internal testing.
- App launches under `com.aiphotoclear.storagecleaner`.
- Onboarding and media permission flow work.
- Clean dashboard shows scanned counts and recoverable space.
- Large videos review opens and shows candidate files when videos exist.
- Similar photos review opens and does not look frozen during scan.
- No crash appears in Android logcat during startup and the first two review flows.
