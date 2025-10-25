# Release notes

## ClickUp sync hardening
- Added conditional requests with ETag tracking for task synchronisation
- Improved rate limit handling using Retry-After headers and structured sync errors
- Surfaced sync telemetry and recovery tools in Settings, including reset by task ID

## Home widgets and reminders
- Introduced a Glance-powered Today widget with due task list, quick add shortcut and completion toggles
- Expanded reminder notifications with grouped alerts, timeline deep link and snooze actions
## Global search and smart filters
- Added full-text search across task titles, notes and subtasks with history in the top app bar
- Introduced smart filters for due today, overdue, undated, high priority, ClickUp-linked and shared tasks
- Enabled quick actions from search results with undo support for completion, due dates and priority updates

## Encrypted Drive backups
- Added end-to-end encrypted backups stored in Google Drive App Data with one-tap export and restore from Settings
- Scheduled automatic daily backups when on Wi-Fi and charging, with status and error visibility in-app
- Implemented AES-GCM payload protection, zipped manifests, retention pruning and unit coverage for crypto and Drive flows
