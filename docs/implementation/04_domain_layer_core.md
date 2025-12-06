# Implementation Phase 4: Domain Layer Core

This document details the implementation of the Domain layer, including models, repository interfaces, and use cases.

## Goal
Establish the business logic core of the application, independent of the UI and frameworks.

## Prompts

### 1. Define Domain Models
> Create data classes in `domain/model/`:
> *   `Task`: Pure Kotlin class, similar to Entity but clean (e.g., using `LocalDateTime` instead of Long, Enums for status/priority).
> *   `TaskQueue`: Represents a grouping of tasks (e.g., "Today", "Overdue").
> *   `Project` (abstraction over Lists/Folders/Spaces).

### 2. Define Repository Interfaces
> Create interfaces in `domain/repository/`:
> *   `TaskRepository`
>     *   `fun getTasksFlow(): Flow<List<Task>>`
>     *   `suspend fun getTask(id: String): Task?`
>     *   `suspend fun createTask(task: Task)`
>     *   `suspend fun updateTask(task: Task)`
>     *   `suspend fun refreshTasks()` (triggers network sync)

### 3. Implement Repository
> Create `data/repository/TaskRepositoryImpl.kt`:
> *   Inject `TaskDao` and `ClickUpApi`.
> *   Implement `getTasksFlow` by returning `taskDao.getAllTasks().map { it.toDomain() }`.
> *   Implement `refreshTasks`:
>     *   Fetch from `ClickUpApi`.
>     *   Map DTOs to Entities.
>     *   Insert/Update `TaskDao` (transactional).
> *   Implement `createTask`:
>     *   Insert into `TaskDao` (marked as unsynced).
>     *   (Optionally) trigger immediate network call or rely on background worker.

### 4. Create Mappers
> Create `data/mapper/TaskMapper.kt`:
> *   `fun TaskEntity.toDomain(): Task`
> *   `fun Task.toEntity(): TaskEntity`
> *   `fun ClickUpTaskDto.toEntity(): TaskEntity`

### 5. Create Use Cases
> Create classes in `domain/usecase/task/`:
> *   `GetTasksUseCase`: Returns filtered/sorted tasks.
> *   `CreateTaskUseCase`: Validates input and calls repo.
> *   `ToggleTaskStatusUseCase`: Handles completion logic.

### 6. Hilt Module for Repository
> Create `core/di/RepositoryModule.kt`:
> *   Bind `TaskRepositoryImpl` to `TaskRepository`.

### 7. Verification
> Create unit tests for Use Cases in `test/java/com/letsdoit/app/domain/usecase/`:
> *   Mock `TaskRepository`.
> *   Test `GetTasksUseCase` filtering logic.
> *   Test `CreateTaskUseCase` validation logic.
