# Locale Rollout

## Phase 1 Stress-Test Locales

- `en`: baseline global English
- `de`: long Western European text and privacy-first copy
- `ar`: RTL layout and mirrored reading order
- `ja`: compact high-trust East Asian copy
- `hi`: text expansion and emerging-market readability

## Phase 2 Full Launch Locales

- `zh-CN`
- `es`
- `pt-BR`
- `ru`
- `ko`
- `fr`
- `id`
- `th`
- `vi`
- `tr`
- `it`
- `el`
- `pl`
- `nl`
- `sv`

## Validation Rules

- No hardcoded user-facing strings in Compose screens.
- No missing string resources in phase 1 locales.
- Arabic must render with RTL direction and correct system mirroring.
- German and Hindi must not overflow primary buttons or cleanup cards.
- Paywall and subscription copy must be localized before any production release.

