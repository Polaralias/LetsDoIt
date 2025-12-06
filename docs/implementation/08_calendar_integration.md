# Implementation Phase 8: Calendar Integration

This document details the integration with the Android Calendar Provider.

## Goal
Sync tasks with due dates to the user's default calendar.

## Prompts

### 1. Permissions
> Add to Manifest:
> *   `READ_CALENDAR`
> *   `WRITE_CALENDAR`
>
> Request these permissions at runtime before enabling the feature.

### 2. Calendar Repository
> Create `data/repository/CalendarRepository.kt`:
> *   `fun getCalendars(): List<CalendarAccount>` (query `CalendarContract.Calendars`).
> *   `fun addEvent(task: Task, calendarId: Long)`:
>     *   Insert into `CalendarContract.Events`.
>     *   Store the returned `eventId` in the `Task` entity (add column `calendarEventId`).
> *   `fun updateEvent(task: Task)`.
> *   `fun deleteEvent(eventId: Long)`.

### 3. Integrate with Task Lifecycle
> Update `SaveTaskUseCase`:
> *   Check if "Sync to Calendar" preference is enabled.
> *   If yes, call `calendarRepository.addEvent` or `updateEvent`.

### 4. Settings UI
> Add to `SettingsScreen`:
> *   Switch "Sync to Calendar".
> *   Dropdown "Select Calendar" (populated by `getCalendars`).

### 5. Verification
> *   Enable calendar sync.
> *   Create a task with a due date.
> *   Open Google Calendar (or system calendar) and verify the event exists.
