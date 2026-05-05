# Google Play Data Safety Worksheet

Last updated: 2026-05-06
App: AI Photo Cleaner
Package: com.aiphotoclear.storagecleaner

This worksheet is a product and engineering source of truth for filling Google Play Data safety. It must be reviewed before public release.

## Data Collection Summary

AI Photo Cleaner processes the user's on-device media library to find storage cleanup opportunities. The product should be represented as a utility app where the primary user value is local media review and storage cleanup.

## Data Types

### Photos And Videos

- Purpose: App functionality.
- Collection: The app requests Android media access to scan photos/videos and show review candidates.
- Sharing: Do not share user media with third parties.
- Processing model: On-device scanning and review.
- User control: Users grant/revoke Android media permissions and must confirm deletion actions.

### App Activity

- Purpose: Analytics, product improvement, diagnostics.
- Collection: Firebase Analytics SDK is integrated.
- Current behavior observed in debug logs: Firebase initializes, but app measurement can be disabled by the app privacy controller.
- Sharing: Google/Firebase acts as analytics service provider when analytics is enabled.
- Required Play Console disclosure: disclose analytics events if collection can be enabled in production.

### Device Or Other IDs

- Purpose: Analytics, diagnostics.
- Collection: Firebase Analytics may use app instance identifiers when enabled.
- Required Play Console disclosure: disclose device or other IDs if analytics is enabled in production.

## Security Practices

- Data is encrypted in transit when Firebase services are used.
- Users can request deletion through app actions for local media items.
- Media deletion is mediated by Android system confirmation where required.

## Play Console Answers To Prepare

- Does the app collect or share required user data types? Yes, if Firebase Analytics is enabled in production.
- Is all user data encrypted in transit? Yes for Firebase network data.
- Can users request data deletion? Local media cleanup is user initiated; analytics data deletion policy depends on Firebase/Google retention and any published privacy policy.
- Does the app process photos/videos? Yes, for app functionality.

## Open Decisions Before Public Release

1. Decide whether Firebase Analytics is opt-in, opt-out, or disabled until consent.
2. Use the published privacy policy URL in Play Console: `https://airs020992.github.io/AI-Photo-Cleaner-Clean-Storage/privacy-policy.html`.
3. Confirm whether ads and billing SDKs are enabled in production; update Data safety if they collect identifiers or diagnostics.
