# Implementation Phase 5: Presentation - Task Management

This document details the implementation of the UI for listing, creating, and editing tasks using Jetpack Compose.

## Goal
Create a responsive and interactive UI for managing tasks, connecting the Domain layer to the user.

## Prompts

### 1. Define UI State
> Create `presentation/home/HomeState.kt`:
> *   `data class HomeState(val tasks: List<Task> = emptyList(), val isLoading: Boolean = false, val error: String? = null)`

### 2. Implement HomeViewModel
> Create `presentation/home/HomeViewModel.kt`:
> *   Inject `GetTasksUseCase`.
> *   Expose `uiState: StateFlow<HomeState>`.
> *   `init { loadTasks() }` collecting the flow from use case.
> *   Functions for user actions: `onTaskChecked(id)`, `onTaskClick(id)`.

### 3. Create Task Item Composable
> Create `presentation/components/TaskItem.kt`:
> *   Receives `Task` model and callbacks (`onClick`, `onCheck`).
> *   Display title, due date, priority indicator.
> *   Use `Checkbox` for completion.
> *   Apply `MaterialTheme` styling.

### 4. Create Home Screen
> Create `presentation/home/HomeScreen.kt`:
> *   Collect `HomeViewModel` state.
> *   Use `Scaffold` with a `FloatingActionButton` (for add task).
> *   Use `LazyColumn` to display `TaskItem`s.
> *   Handle loading and error states.

### 5. Create Task Detail Screen
> Create `presentation/taskdetails/TaskDetailScreen.kt`:
> *   ViewModel: `TaskDetailViewModel` (loads task by ID).
> *   UI: Editable fields for Title, Description, Due Date (DatePicker), Priority (Dropdown/Chips).
> *   "Save" button in TopAppBar.

### 6. Verification
> Run the app.
> *   Verify "Home" shows the list from the database (initially empty).
> *   Verify FAB navigates to a creation screen (or reuse Detail screen with null ID).
> *   Verify saving a task updates the list on return.
