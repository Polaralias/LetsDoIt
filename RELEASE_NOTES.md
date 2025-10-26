# Release notes

## Accessibility and localisation readiness
- Added TalkBack-friendly semantics for task, board and timeline cards, including alternative reordering actions and content descriptions for all controls.
- Ensured touch targets meet 48dp guidance with high contrast palette toggle, accessible theme presets and locale-aware date formatting.
- Localised strings with en-GB defaults, added en scaffold resources and introduced instrumentation coverage for labels plus large-font snapshot tests.

## ClickUp sync hardening
- Added conditional requests with ETag tracking for task synchronisation
- Improved rate limit handling using Retry-After headers and structured sync errors
- Surfaced sync telemetry and recovery tools in Settings, including reset by task ID
- Implemented server-wins reconciliation with remote freshness tracking and conditional pushes for up-to-date overrides
- Respected Retry-After delays by scheduling deferred work, persisting the window and surfacing it in Settings
- Clarified the two-minute freshness rule with server precedence and recorded Retry-After windows for visibility in Settings

## Build infrastructure
- Bundled local Gradle plugin markers so builds run in constrained network environments.

## Diagnostics and support bundle
- Added an opt-in diagnostics toggle capturing crashes with a rolling log buffer when enabled.
- Added a redacted support bundle export with device metadata and share sheet delivery for support teams.

## Home widgets and reminders
- Introduced a Glance-powered Today widget with due task list, quick add shortcut and completion toggles
- Expanded reminder notifications with grouped alerts, timeline deep link and snooze actions
## Global search and smart filters
- Added full-text search across task titles, notes and subtasks with history in the top app bar
- Introduced smart filters for due today, overdue, undated, high priority, ClickUp-linked and shared tasks
- Enabled quick actions from search results with undo support for completion, due dates and priority updates

## Theme accents generator
- Added on-device accent pack storage with preview and removal tools in Settings
- Introduced prompt-driven accent generation with preset ideas, OpenAI image integration and cost warning messaging
- Enabled caching of generated packs to avoid duplicate API calls and expanded tests covering hashing and UI flows

## Encrypted Drive backups
- Added end-to-end encrypted backups stored in Google Drive App Data with one-tap export and restore from Settings
- Scheduled automatic daily backups when on Wi-Fi and charging, with status and error visibility in-app
- Implemented AES-GCM payload protection, zipped manifests, retention pruning and unit coverage for crypto and Drive flows

## Performance and baseline optimisations
- Added Paging 3 backed task and timeline feeds with lazy Compose collection and debounced item placement for smooth scrolling up to 10k items
- Indexed Room entities on completion state, due dates, columns and priority to keep filters quick at scale
- Introduced cached accent sticker loading with ImageBitmap reuse to reduce bitmap churn and memory pressure
- Added a baseline profile module covering launch, list, board, timeline and task creation journeys, plus macro and micro benchmarks for scrolling and bulk import parsing
