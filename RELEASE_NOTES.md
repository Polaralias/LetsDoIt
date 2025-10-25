# Release notes

## ClickUp sync hardening
- Added conditional requests with ETag tracking for task synchronisation
- Improved rate limit handling using Retry-After headers and structured sync errors
- Surfaced sync telemetry and recovery tools in Settings, including reset by task ID
