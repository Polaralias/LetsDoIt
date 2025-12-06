# Implementation Phase 9: Background Sync

This document details the setup of background synchronization using WorkManager.

## Goal
Ensure tasks are synced with ClickUp even when the app is not in the foreground.

## Prompts

### 1. Create Worker
> Create `data/worker/SyncWorker.kt`:
> *   Extend `CoroutineWorker`.
> *   Inject `TaskRepository` (using `@HiltWorker`).
> *   In `doWork()`:
>     *   Call `taskRepository.syncUnsyncedTasks()` (pushes local changes).
>     *   Call `taskRepository.refreshTasks()` (pulls remote changes).
>     *   Return `Result.success()`.

### 2. Setup WorkManager Factory
> Ensure Hilt is configured for WorkManager (implement `Configuration.Provider` in Application class if using custom configuration, or use Hilt's default if standard).

### 3. Schedule Work
> Create `domain/sync/SyncScheduler.kt`:
> *   `fun schedulePeriodicSync()`:
>     *   `PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)`.
>     *   `setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())`.
>     *   `WorkManager.getInstance(context).enqueueUniquePeriodicWork(...)`.

### 4. Trigger on App Start
> Call `SyncScheduler.schedulePeriodicSync()` in `LetsDoItApp.onCreate`.

### 5. Verification
> *   Turn off internet.
> *   Create a task.
> *   Turn on internet.
> *   Wait for the worker to run (or force it via Inspector).
> *   Check ClickUp to see if the task appeared.
