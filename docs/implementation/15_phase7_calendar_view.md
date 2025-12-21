# Implementation Phase 15: Phase 7 Calendar View

This document details the implementation of the Calendar View (Agenda View), merging local tasks and system calendar events.

## Goal
Provide a unified view of tasks and calendar events to help users plan their day.

## Features

### 1. Hybrid Agenda View
> **Goal**: Display a list of items (Tasks + Calendar Events) sorted by time.
>
> *   **Domain Model**: `CalendarEvent` (id, title, description, start, end, color, allDay).
> *   **Repository**: Add `getEvents(start: Long, end: Long)` to `CalendarRepository`.
> *   **Use Case**: `GetCalendarEventsUseCase` to fetch events for a date range.
> *   **UI**: `CalendarScreen` displaying `AgendaItem` (sealed class for Task or Event).

### 2. Calendar Integration
> **Goal**: Read events from Android Calendar Provider.
>
> *   **Implementation**: `CalendarRepositoryImpl` queries `CalendarContract.Events`.
> *   **Permissions**: Handle `READ_CALENDAR` permission. If missing, show "Request Permission" UI.
> *   **Scope**: Fetch events from all visible calendars.

### 3. Navigation
> **Goal**: Access the Calendar View from the Home Screen.
>
> *   **Route**: `calendar` via `Screen.Calendar`.
> *   **Entry Point**: Add a Calendar icon to the `HomeScreen` TopAppBar.

## Verification
> *   Unit tests for UseCase and ViewModel.
> *   Manual verification:
>     *   Grant Calendar permission.
>     *   Add a task with a due date.
>     *   Verify both appear in the Calendar View.
