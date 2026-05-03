# Similar Screenshots Analytics

## Purpose

Measure whether users can find, trust, and complete the Similar photos cleanup flow without collecting photo names, file paths, content URIs, image hashes, or thumbnails.

Analytics is privacy-gated by the in-app Product analytics switch. Firebase collection and custom events stay disabled until the user enables that switch.

## Funnel

1. `similar_screenshots_entry_tapped`
2. `similar_screenshots_scan_completed`
3. `similar_screenshots_review_shown`
4. `similar_screenshots_selection_changed`
5. `similar_screenshots_continue_tapped`
6. `similar_screenshots_delete_requested`
7. `similar_screenshots_system_delete_result`
8. `similar_screenshots_post_delete_action`

`similar_screenshots_rescan_tapped` measures recovery after empty or stale results.

## Event Contract

| Event | When | Parameters |
| --- | --- | --- |
| `similar_screenshots_entry_tapped` | User opens Similar photos from Photos. | `groups_loaded` Boolean, `group_count` Long, `recoverable_bytes` Long, `status` String |
| `similar_screenshots_rescan_tapped` | User taps Rescan photos from an empty or stale review state. | `current_group_count` Long, `status` String |
| `similar_screenshots_scan_completed` | Similar screenshot scan finishes. | `elapsed_ms` Long, `empty_result` Boolean, `group_count` Long, `recoverable_bytes` Long, `screenshot_count` Long, `status` String |
| `similar_screenshots_review_shown` | Similar screenshots review screen receives groups and initial selection state. | `group_count` Long, `priority_groups` Long, `recoverable_bytes` Long, `selected_bytes` Long, `selected_count` Long, `status` String |
| `similar_screenshots_selection_changed` | User changes review selection from row toggle, group clear, suggested reset, or preview toggle. | `action` String, `priority_groups` Long, `selected_bytes` Long, `selected_count` Long, `total_groups` Long |
| `similar_screenshots_continue_tapped` | User continues from review toward Android system delete confirmation. | `priority_groups` Long, `selected_bytes` Long, `selected_count` Long, `total_groups` Long |
| `similar_screenshots_delete_requested` | User confirms the in-app delete dialog and the app attempts to open Android system confirmation. | `missing_access_count` Long, `priority_groups` Long, `selected_bytes` Long, `selected_count` Long, `system_dialog_available` Boolean |
| `similar_screenshots_system_delete_result` | Android system delete confirmation returns. | `confirmed` Boolean, `priority_groups` Long, `selected_bytes` Long, `selected_count` Long |
| `similar_screenshots_post_delete_action` | User taps the post-delete result card CTA after Android delete result reconciliation. | `action` String, `has_priority_groups` Boolean, `remaining_groups` Long, `remaining_recoverable_bytes` Long |

## Health Questions

- Entry intent: how often users open Similar photos from Photos.
- Responsiveness: median and p95 `elapsed_ms` for `similar_screenshots_scan_completed`.
- Result quality: `empty_result` rate and `group_count` distribution.
- Review exposure: `review_shown / scan_completed` when `empty_result=false`.
- Trust edits: `selection_changed / review_shown` and action distribution.
- Commit intent: `continue_tapped / review_shown`.
- System friction: `delete_requested` with `system_dialog_available=false`, then confirmed vs cancelled `system_delete_result`.
- Post-delete continuation: `post_delete_action / system_delete_result` and action distribution.

## Privacy Rules

- Do not log content URI, display name, path, folder, file hash, image dimensions that identify a file, thumbnail, or OCR text.
- Keep event and parameter names GA4-safe: lowercase letters, numbers, underscores, max 40 characters.
- Add or rename any event only by updating `SimilarScreenshotAnalyticsContract` and its unit tests.
