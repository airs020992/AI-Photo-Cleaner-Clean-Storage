# AI Photo Cleaner Design

Date: 2026-05-03

## Product Direction

Build an Android utility app for global users who are running out of phone storage because their gallery contains duplicate photos, similar shots, blurry images, screenshots, and large videos.

Working title:

`AI Photo Cleaner: Clean Storage`

The first release should focus on a clear promise: help users find what is wasting space, preview it safely, and clean it with confidence. The app must avoid deceptive "phone booster", "battery repair", fake antivirus, fake system warning, or automatic deletion behavior.

## Why This Category

The category is selected because cleaning and storage tools show strong revenue signals while remaining feasible for a small developer team.

Observed market signals:

- AppBrain's United States Google Play Tools Top Grossing ranking lists `Cleanup: Phone Storage Cleaner` at #3 and `AI Cleaner - Phone Cleaner` at #4.
- `Cleanup: Phone Storage Cleaner` also appears in the broader United States Google Play Top Grossing Applications ranking, showing the product can compete beyond its narrow category.
- Sensor Tower's Q4 2025 utilities data reported `Cleanup: Phone Storage Cleaner` reaching approximately `$1.4M/week` in revenue in December 2025.
- The same pattern appears on iOS utilities rankings, where cleaning apps such as `Cleanup`, `Cleaner Guru`, `AI Cleaner`, and `Photo Cleaner: Swipewipe` rank strongly.

VPN and antivirus products also monetize well, but they require infrastructure, security trust, compliance, and brand credibility that are poor first-project fits for an individual overseas tools developer.

## User Pain

Primary user pains:

- The phone is full at the exact moment the user wants to take a photo, record a video, install an app, or receive a file.
- The user knows the gallery contains junk, but manually reviewing thousands of photos is tedious.
- The user fears deleting the wrong memory, so "safe preview" matters more than aggressive deletion.
- Videos consume large storage, but the user does not know which ones are biggest or whether compression will visibly damage quality.
- The user wants a visible result before paying.

The product should make the emotional arc concrete:

1. "My phone is full."
2. "The app found exactly where the waste is."
3. "I can preview before deleting."
4. "I recovered visible storage."
5. "Paying unlocks the faster version of something I already trust."

## Target Markets And Languages

The app should support these 20 locales in UI, store listing, onboarding, paywall, permission explanation, and subscription copy:

| Market | Locale |
| --- | --- |
| United States / global English | `en-US` |
| Simplified Chinese | `zh-CN` |
| Spanish | `es` |
| Brazilian Portuguese | `pt-BR` |
| Russian | `ru` |
| Japanese | `ja` |
| Korean | `ko` |
| German | `de` |
| French | `fr` |
| Arabic | `ar` |
| Indonesian | `id` |
| Thai | `th` |
| Vietnamese | `vi` |
| Turkish | `tr` |
| Italian | `it` |
| Greek | `el` |
| Polish | `pl` |
| Dutch | `nl` |
| Swedish | `sv` |
| Hindi | `hi` |

Arabic must support RTL layout. Text length expansion must be tested for German, Russian, Portuguese, Hindi, and Arabic.

## Platform And Technology

Use the current mainstream native Android stack:

- Kotlin
- Jetpack Compose
- Material 3
- MVVM with clean boundaries between UI, domain, and data layers
- Coroutines and Flow
- Room for scan cache and detected groups
- DataStore for settings and onboarding state
- Hilt for dependency injection
- Coil for thumbnails
- Google Play Billing for subscriptions and one-time products
- Google Mobile Ads SDK for rewarded ads and carefully limited interstitial ads
- Firebase Analytics, Crashlytics, and Remote Config
- Target SDK: Android 15 / API 35 or newer, matching current Google Play target API requirements for new submissions

Minimum SDK can be Android 9 / API 28 unless media access or performance testing shows a better cutoff. Android 13+ media permission handling must use the modern photo/video permissions model.

## MVP Scope

The first version includes:

1. Permission and trust onboarding
2. Storage scan dashboard
3. Duplicate photo detection
4. Similar photo detection
5. Blurry photo detection
6. Screenshot cleanup
7. Large video finder
8. Video compression
9. Duplicate contacts finder
10. Safe preview and explicit user confirmation before deletion
11. Paywall for unlimited advanced cleaning
12. Rewarded ads for one-time advanced actions
13. Analytics events for scan, preview, deletion, ad reward, and subscription funnel
14. 20-language UI and store listing support

Not included in MVP:

- VPN
- Antivirus
- Device booster
- Battery repair
- CPU cooling
- Hidden background deletion
- Cloud backup
- User account system
- Social sharing

## Core User Flow

### First Launch

The app opens directly into a storage problem framing screen, not a marketing landing page. It asks for photo/video access only after explaining that scanning happens so the user can review large, duplicate, similar, and blurry media.

The permission screen must be calm and specific:

- Explain what access is needed.
- Explain that the app will not delete anything without confirmation.
- Explain that scanning may take time for large galleries.

### Scan

After permission, the app scans media and shows progress:

- Photos scanned
- Videos scanned
- Potential space to recover
- Categories being analyzed

The scan should stream partial results so users do not stare at an empty loading screen.

### Results Dashboard

The dashboard prioritizes action cards:

- Duplicate photos
- Similar photos
- Blurry photos
- Screenshots
- Large videos
- Duplicate contacts

Each card shows:

- Item count
- Estimated storage recoverable
- A small preview strip
- A clear action button

### Review

Each cleanup category opens a review screen with grouped items. The app may suggest what to keep, but the user must be able to inspect and change selections.

Key interaction:

- Group by duplicate/similar cluster
- Highlight "recommended keep"
- Let users expand a group
- Show image metadata when useful
- Avoid frightening copy

### Clean

Before deleting, show a confirmation summary:

- Number of items selected
- Estimated storage recovered
- Clear note that deletion uses Android's system media deletion flow when required

If Android requires system confirmation, the app should explain that it is a normal Android protection step.

## Monetization

Free tier:

- Full scan
- Preview all results
- Limited manual cleaning
- Limited video compression preview
- Ads on non-critical surfaces

Rewarded ads:

- Unlock one batch clean
- Unlock one video compression
- Unlock one smart selection pass

Subscription:

- Weekly: `$4.99`
- Monthly: `$9.99`
- Yearly: `$39.99`

Premium unlocks:

- Unlimited batch clean
- Smart best-photo selection
- Unlimited video compression
- Automatic scan reminders
- No ads

Paywall timing:

- Do not block initial scan.
- Show first paywall only after the user has seen recoverable storage.
- Allow a clear close path to remain compliant with subscription and free-trial policies.
- Explain trial duration, renewal price, and cancellation path clearly if a trial is offered.

## Ad Strategy

Ads must support monetization without damaging trust:

- Rewarded ads are preferred because the exchange is explicit.
- Interstitials can appear after non-destructive actions, not during permission, deletion, or payment moments.
- No ads may mimic system warnings.
- No ad should imply the device is infected, overheated, or broken.

## Compliance Boundaries

The app must comply with Google Play deceptive behavior, subscription, permissions, and target API requirements.

Required boundaries:

- Store listing must accurately describe actual features.
- No fake scan results.
- No fake virus/security warnings.
- No automatic deletion without user confirmation.
- No hidden behavior.
- No excessive permissions.
- Subscription terms must be localized and clear.
- Free trial terms must explain conversion, price, billing period, and cancellation.

## Architecture

Suggested modules:

- `app`: Android entry point and DI setup
- `core:ui`: shared Compose components and theme
- `core:i18n`: locale helpers and formatting
- `core:billing`: Google Play Billing wrapper
- `core:ads`: AdMob wrapper
- `core:analytics`: analytics event API
- `core:permissions`: media/contact permission handling
- `feature:onboarding`: first-run trust and permission flow
- `feature:scan`: media scanning and progress UI
- `feature:dashboard`: scan result dashboard
- `feature:photos`: duplicate, similar, blurry, and screenshot review
- `feature:videos`: large video review and compression
- `feature:contacts`: duplicate contacts review
- `feature:paywall`: subscription and rewarded unlock flows
- `data:media`: Android MediaStore data access
- `data:contacts`: Contacts provider data access
- `domain:cleaning`: clustering, scoring, and recommendation use cases

## Detection Approach

Duplicate photos:

- Compare exact file metadata and content hash where practical.
- Cache signatures to avoid rescanning unchanged media.

Similar photos:

- Generate perceptual hash from thumbnails.
- Cluster images by hamming distance.
- Prefer local processing for privacy and cost control.

Blurry photos:

- Use local sharpness scoring.
- Treat blur as a suggestion, never an automatic delete decision.

Large videos:

- Sort by size and duration.
- Estimate compression savings before processing.

Video compression:

- Use Android media APIs or a carefully vetted library.
- Preserve original unless the user confirms replacement or deletion.

Contacts:

- Detect exact and fuzzy duplicates by normalized phone, email, and name.
- Require user confirmation before merge.

## Design Direction

The visual style should feel trustworthy, utilitarian, and fast. This is not a playful app or a marketing page. Users arrive with a problem and need confidence.

Design principles:

- Lead with storage recovered and clear next action.
- Use restrained color with strong contrast.
- Use category icons, progress rings, and preview thumbnails.
- Avoid alarmist red unless an action is destructive.
- Use green or blue only for success/trust, not as the entire palette.
- Make delete actions explicit and reversible where possible.
- Keep text compact enough for repeated use.

Figma should be used to design and validate these screens:

1. First launch / permission trust screen
2. Scan progress screen
3. Dashboard with cleanup categories
4. Duplicate photo group review
5. Similar photo smart selection review
6. Large video review and compression estimate
7. Delete confirmation sheet
8. Paywall
9. Rewarded ad unlock prompt
10. Settings and subscription management

Each screen should include mobile layouts for English, German, Arabic RTL, Hindi, and Japanese to catch text expansion and layout direction issues early.

## Regional Design Adaptation

Localization is not only translation. The product should keep one consistent core UX while adapting visual density, tone, trust cues, and store screenshots for regional expectations.

Recommended regional treatment:

- United States and English-first markets: direct value framing, clean dashboard, clear storage recovered metric, restrained subscription copy, strong privacy reassurance.
- Latin America and Brazil: more benefit-forward screenshot copy, visible before/after storage recovery, warmer success states, clear value-for-money annual pricing.
- Japan and Korea: compact but polished layouts, high precision in microcopy, less aggressive emotional language, stronger emphasis on safety and user control before deletion.
- Germany, France, Netherlands, Sweden, and broader Western Europe: privacy-first permission copy, minimal visual noise, clear subscription cancellation wording, restrained colors.
- Arabic markets: full RTL layout validation, larger tap targets where text expands, culturally neutral media examples, avoid layouts that assume left-to-right progress.
- India, Indonesia, Vietnam, Thailand, Turkey, Poland, Russia, Greece, and Italy: efficient onboarding, localized storage examples, strong free-result preview before paywall, avoid paywall appearing before visible scan value.

Figma design reviews must compare at least five representative variants:

- `en-US`: baseline global layout
- `de`: long Western European text stress test
- `ar`: RTL layout and mirrored navigation
- `ja`: compact high-trust East Asian layout
- `hi`: text expansion and emerging-market readability test

## Analytics

Track:

- Permission screen viewed
- Permission accepted / denied
- Scan started
- Scan completed
- Recoverable storage found
- Category opened
- Items selected
- Delete confirmation shown
- Delete completed
- Paywall shown
- Paywall closed
- Subscription started
- Trial started
- Rewarded ad shown
- Rewarded ad completed
- Compression started / completed / failed

Do not log filenames, contact names, image contents, or sensitive media metadata.

## Testing

Functional tests:

- Media scan on empty gallery
- Media scan on large gallery
- Duplicate grouping accuracy
- Similar grouping threshold behavior
- Blur scoring threshold behavior
- Delete flow with Android system confirmation
- Contact merge preview
- Billing purchase restore
- Rewarded ad unlock state

Localization tests:

- All 20 locales compile with no missing strings.
- Arabic RTL layout works.
- Long German and Russian strings do not overflow.
- Subscription prices and terms are localized.

Performance tests:

- Scan 1,000 media files
- Scan 10,000 media files
- Resume scan after app restart
- Avoid UI blocking during hashing and thumbnail generation

Compliance tests:

- No copy claims impossible functionality.
- No fake security wording.
- Paywall has a clear close path.
- Trial and subscription terms are visible and localized.

## Release Plan

Phase 1:

- Build MVP in English and 4 stress-test locales: German, Arabic, Hindi, Japanese.
- Internal test track.
- Validate scanning performance and deletion safety.

Phase 2:

- Add all 20 locales.
- Prepare localized Play Store listing and screenshots.
- Closed test with target market devices.

Phase 3:

- Launch in English-first markets.
- Monitor conversion, crashes, ANR, review themes, and refund signals.

Phase 4:

- Roll out remaining locales and markets.
- A/B test paywall timing, annual discount, and rewarded ad unlocks.

## Product Decisions

- Contacts cleanup should ship in version 1.1 unless the first implementation finishes ahead of schedule. The first Play Store submission should keep the core story focused on photos, videos, and storage recovery.
- Video compression should create a compressed copy first and never replace the original by default. Users can delete the original only through an explicit confirmation step after previewing the result.
- The first subscription offer should use an annual discount and optional introductory price, not a free trial. This reduces compliance copy complexity across 20 languages while the funnel is still unproven.
- Figma work should start in a new design file unless the user provides an existing team file. The first Figma deliverable should be a mobile app flow board covering onboarding, scan, dashboard, review, confirmation, paywall, and settings.
