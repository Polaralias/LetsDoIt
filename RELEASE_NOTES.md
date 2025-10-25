# Release notes

## ClickUp sync hardening
- Added conditional requests with ETag tracking for task synchronisation
- Improved rate limit handling using Retry-After headers and structured sync errors
- Surfaced sync telemetry and recovery tools in Settings, including reset by task ID

## Global search and smart filters
- Added full-text search across task titles, notes and subtasks with history in the top app bar
- Introduced smart filters for due today, overdue, undated, high priority, ClickUp-linked and shared tasks
- Enabled quick actions from search results with undo support for completion, due dates and priority updates
