# Play Console Form Answers

Last updated: 2026-05-06
App: AI Photo Cleaner
Package: `com.aiphotoclear.storagecleaner`

Use this as the fill-in source for the first internal test release. Re-check before public production rollout.

## App Access

- App access restriction: No special access required for testers after installing.
- Login required: No.
- Notes: The app requires Android media permission to scan local photos and videos.

## Privacy Policy

- URL: `https://airs020992.github.io/AI-Photo-Cleaner-Clean-Storage/privacy-policy.html`
- Contact email: `airs020992@gmail.com`

## Ads Declaration

- Recommended answer: Yes, this app contains ads.
- Reason: The app currently packages Google Mobile Ads SDK through `core:ads`, and the manifest includes an AdMob application id.
- Current product behavior: Ads should be treated as not yet production-monetized until placements, consent, and real ad unit ids are finalized.
- Public launch requirement: Replace test ad ids with production ids only after consent and paid/organic funnel strategy is ready.

## Data Safety

### Data Collection

- Does the app collect or share user data? Yes.
- Is all collected user data encrypted in transit? Yes for Firebase and Google SDK network traffic.
- Does the app provide a way for users to request data deletion? Yes for local media actions; analytics deletion follows Google/Firebase controls and policy.

### Data Types To Disclose

#### Photos and videos

- Collected: Yes, for app functionality.
- Shared: No.
- Purpose: App functionality.
- Required or optional: Required for media cleanup features.
- Processing: On-device scanning and review. Core cleanup does not upload media to app-owned servers.

#### App activity

- Collected: Yes, when Product analytics is enabled.
- Shared: Yes, with Google/Firebase as service provider.
- Purpose: Analytics, app functionality, diagnostics, product improvement.
- Required or optional: Optional from a product-control perspective; collection is controlled by the app privacy toggle.

#### Device or other IDs

- Collected: Yes, when Firebase Analytics or Google Mobile Ads SDK is active.
- Shared: Yes, with Google/Firebase/Google Mobile Ads as service providers.
- Purpose: Analytics, diagnostics, advertising/monetization measurement.
- Required or optional: Optional for analytics; advertising identifiers depend on ad SDK behavior and consent configuration.

#### Crash logs / diagnostics

- Collected: No dedicated Crashlytics collection is configured in this release.
- Note: If Crashlytics is enabled later, update this form and privacy policy before release.

## Content Rating

- App category: Utility / productivity.
- Violence: No.
- Sexual content: No.
- Profanity: No.
- Controlled substances: No.
- Gambling: No.
- User-generated content sharing: No.
- Social features/chat: No.
- Location sharing: No.
- Personal data disclosure in app UI: No, except local media filenames/thumbnails that stay on device.
- Target audience: Adults / general audience, not specifically directed to children.

## Target Audience

- Primary audience: Adults and general phone users who want to recover storage safely.
- Children-directed app: No.
- Recommended age group: 18+ for initial release until ads, analytics consent, and policy posture are fully reviewed.

## Internal Testing

- Track: Internal testing.
- Release name: `0.1.0 (1)`
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- Release notes: `docs/play-console/internal-test-release-notes.md`
- Tester requirement: Gmail accounts added to the internal testing list.

## Store Listing

- Short description: `Find large videos, similar photos, and screenshots before deleting anything.`
- Full description: `docs/play-console/store-listing-draft.md`
- App icon: `docs/play-console/assets/app-icon-512.png`
- Feature graphic: `docs/play-console/assets/feature-graphic-1024x500.png`
- Phone screenshots: `docs/play-console/screenshots/`

## Pre-Public-Launch Review Items

- Confirm final analytics consent behavior.
- Confirm whether ads are actually shown in the production build.
- Replace any AdMob test ids before public monetization.
- Confirm billing products and subscription disclosures before enabling paid flows.
- Re-run Play internal test installation from the Play Store distribution path.
