# Play Console Upload Manifest

Last updated: 2026-05-06

This manifest pins the exact build and store assets prepared for the first internal testing upload.

## Build Identity

- App: AI Photo Cleaner
- Package: `com.aiphotoclear.storagecleaner`
- Version name: `0.1.0`
- Version code: `1`
- Source commit: `6d4ff6d57d8160466ea9b53b5e809528213ce6a8`
- Track: Internal testing

## Release Bundle

| File | Size | SHA256 |
| --- | ---: | --- |
| `app/build/outputs/bundle/release/app-release.aab` | 18,150,844 bytes | `0DD05C3622BEDE8AC05D439A8134B872D0083F01ED2749D482D439F80BB95B73` |

## Store Assets

| File | Size | SHA256 |
| --- | ---: | --- |
| `docs/play-console/assets/app-icon-512.png` | 9,048 bytes | `FCE8BD49DE66D8E9ADFFECE711C85F4992C1414700EFE2A7AA2AAB001CA1453F` |
| `docs/play-console/assets/feature-graphic-1024x500.png` | 20,986 bytes | `6393FBB4B24DCDA358BA1CF25485AE49225F9DF70162B6A80EA7E28FEAD671E8` |

## Phone Screenshots

| File | Size | SHA256 |
| --- | ---: | --- |
| `docs/play-console/screenshots/01-clean-dashboard.png` | 238,419 bytes | `5FF876BE2EE586ADDF51D8D5DCBA747660C377AA6D71AF7226600411207AA60A` |
| `docs/play-console/screenshots/02-videos-tab.png` | 269,143 bytes | `9E0CB0DCBF03F89509DF699CD1C1F49F039B86571B8E3864776721340768DE07` |
| `docs/play-console/screenshots/03-large-videos-entry.png` | 317,343 bytes | `F93851D0A4D8C799239B25EDBF570A21399E07A0A0F3877D1B9D99ACEA3DBDC4` |
| `docs/play-console/screenshots/04-settings-tab.png` | 342,315 bytes | `DD6356E1DCA463614518489B840D187F0D6CA7CE2C9770E874E2BBF7E838F0C1` |

## Console Fill-In Sources

- Privacy policy URL: `https://airs020992.github.io/AI-Photo-Cleaner-Clean-Storage/privacy-policy.html`
- Store listing copy: `docs/play-console/store-listing-draft.md`
- Internal release notes: `docs/play-console/internal-test-release-notes.md`
- Data safety and app content answers: `docs/play-console/play-console-form-answers.md`
- Play-distributed build validation: `docs/play-console/internal-test-validation.md`

## Local Verification

- Build command: `.\gradlew :app:assembleDebug :app:testDebugUnitTest :app:bundleRelease`
- Latest result: passed.
- Gradle task count: 589 actionable tasks.
- Connected test device: `1A071FDEE00538`
- Local app process observed: `12220`
- Foreground activity observed: `com.aiphotoclear.storagecleaner/com.air.cleaner.MainActivity`
- Recent logcat check: no `FATAL EXCEPTION` / `AndroidRuntime` crash match in the sampled logs.
- Git tracked-file check: no `google-services.json`, signing keystore, `local-signing.properties`, AAB, or APK tracked.

## Known Manual Step

The in-app browser can read the Play Console tab URL and title, but DOM/screenshot automation currently fails because the local browser app-server path is unavailable. Upload the AAB and store assets manually, then run the Play-distributed validation plan.
