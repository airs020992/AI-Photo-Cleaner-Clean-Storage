# AI Photo Cleaner Figma Mobile Flow Brief

## Goal

Design the first production-ready Android mobile flow for `AI Photo Cleaner: Clean Storage`.

The design must solve a concrete utility problem: users are out of storage, overwhelmed by messy galleries, and afraid of deleting the wrong memory. The app should earn trust before asking for payment.

## Core User Pain

Users open this app in moments of friction:

- The phone cannot take another photo or install an app because storage is full.
- The gallery has thousands of photos, screenshots, and videos, but manual cleanup feels impossible.
- The user suspects there are duplicates but cannot confidently decide what to delete.
- Large videos consume storage, but users fear compression will ruin quality.
- Users do not want to pay before seeing proof that the app found real recoverable space.

Every screen should reduce one of these anxieties.

## Required Screens

1. First launch and permission trust screen
2. Media permission rationale screen
3. Scan progress screen
4. Dashboard with cleanup category cards
5. Duplicate photo group review
6. Similar photo smart selection review
7. Blurry photo review
8. Screenshots cleanup review
9. Large video review and compression estimate
10. Delete confirmation bottom sheet
11. Success screen after cleaning
12. Paywall after visible scan value
13. Rewarded ad unlock prompt
14. Settings and subscription management

## Emotional User Journey

The design should move the user from storage anxiety to safe control:

1. "My phone is full."
2. "The app found exactly where space is wasted."
3. "I can preview before deleting."
4. "I recovered visible storage."
5. "Premium saves time after I already saw value."

## Global Visual Direction

- Trustworthy utility app, not a playful social app.
- Functional and scan-friendly, not a marketing landing page.
- No alarmist red except destructive confirmation.
- Use sample photo/video preview tiles, not abstract decoration.
- Put storage recovered and next action above decorative content.
- Paywall appears only after scan value is visible.
- Delete actions must feel deliberate, reversible where possible, and never hidden.
- Avoid fake system-warning styling, virus language, battery repair language, or booster tropes.

## First-Screen Requirements

The first screen should not sell a subscription. It should make the user comfortable granting access.

Must communicate:

- The app scans photos and videos to find storage waste.
- Nothing is deleted without confirmation.
- The user can review everything before cleaning.
- The app is useful even before payment because the scan result is free.

Primary CTA:

- `Scan my photos`

Secondary trust line:

- `You review everything before deleting.`

## Dashboard Requirements

The dashboard should answer three questions within two seconds:

- How much can I free?
- What is wasting space?
- What should I do first?

Required dashboard modules:

- Total recoverable storage summary
- Duplicate photos card
- Similar photos card
- Blurry photos card
- Screenshots card
- Large videos card
- Premium / rewarded unlock entry only after result cards are visible

## Review Screen Requirements

Review screens are where trust is won or lost.

Must include:

- Grouped media previews
- Clear recommended keep item
- Explicit selection state
- Storage saved estimate
- Select all / deselect all
- Safe confirmation before deletion

Do not include:

- Auto-delete wording
- Fear-based labels
- Hidden selection defaults that delete memories without user attention

## Paywall Requirements

The paywall should frame premium as speed and convenience, not access to the truth.

Premium unlocks:

- Unlimited batch cleaning
- Smart best-photo selection
- Unlimited video compression
- No ads
- Automatic cleanup reminders

Paywall timing:

- Never before first permission request.
- Never before scan results.
- Best placement: after the user opens a category and attempts a batch clean or smart selection.

Required subscription clarity:

- Weekly, monthly, and yearly options
- Annual value framing
- Clear close path
- Restore purchases
- Cancellation wording

## Figma Output Requirements

Create a mobile flow board with:

- 360 x 800 Android baseline frames
- 412 x 915 large Android frames for text expansion checks
- `en-US`, `de`, `ar`, `ja`, and `hi` representative variants
- Componentized action cards, bottom sheets, progress panels, media group rows, and paywall option rows
- Notes beside each screen explaining which user pain the screen addresses

