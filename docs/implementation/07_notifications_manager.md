# Implementation Phase 7: Notifications Manager

This document details the implementation of local notifications and reminders.

## Goal
Schedule and display system notifications for tasks at their due time.

## Prompts

### 1. Setup Notification Channel
> Create `core/notification/NotificationHelper.kt`:
> *   Function `createNotificationChannel(context)` called in `Application.onCreate`.
> *   Channel ID: "task_reminders", Importance: HIGH.

### 2. Create BroadcastReceiver
> Create `core/receiver/AlarmReceiver.kt`:
> *   Annotate with `@AndroidEntryPoint`.
> *   Inject `TaskRepository` (to fetch task details if needed, though usually data is passed in Intent).
> *   In `onReceive`:
>     *   Build Notification (Title, Content, PendingIntent to open App).
>     *   `NotificationManager.notify(taskId.hashCode(), notification)`.

### 3. Implement Scheduler
> Create `domain/alarm/AlarmScheduler.kt`:
> *   Inject `Context`.
> *   `fun scheduleAlarm(task: Task)`:
>     *   Get `AlarmManager`.
>     *   Create `PendingIntent` pointing to `AlarmReceiver`.
>     *   `alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, task.dueDate, pendingIntent)`.
> *   `fun cancelAlarm(task: Task)`.

### 4. Integrate with Repository/UseCase
> Update `SaveTaskUseCase`:
> *   After saving to DB, call `alarmScheduler.scheduleAlarm(task)` if it has a due date.

### 5. Permissions
> Update `AndroidManifest.xml`:
> *   `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (Android 13+).
> *   `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />`.
> *   Handle runtime permission request in UI.

### 6. Verification
> *   Create a task due in 1 minute.
> *   Close the app.
> *   Wait for the notification to appear.
