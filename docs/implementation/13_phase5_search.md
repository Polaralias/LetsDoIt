# Implementation Phase 13: Phase 5 Search

This document details the features for Phase 5: Search & Filtering.

## Goal
Enable users to quickly find tasks by title, description, or other criteria.

## Features

### 1. Search Screen
> **Goal**: A dedicated screen or bar to search tasks.
>
> *   **Search Bar**: Input text to filter tasks. (Implemented)
> *   **Results List**: Real-time filtering of tasks. (Implemented)
> *   **Filter Options**: Filter by Status, Priority. (Implemented)

## Implementation Steps

1.  **Data Layer** (Completed):
    *   Update `TaskDao` with a search query using `LIKE`.

2.  **Domain Layer** (Completed):
    *   Create `SearchTasksUseCase`.
    *   Add `SearchFilter` model.
    *   Implement in-memory filtering for Status and Priority.

3.  **Presentation Layer** (Completed):
    *   Create `SearchViewModel`.
    *   Create `SearchScreen` with `SearchBar` (Material 3).
    *   Add Filter Chips for Status (Active, Completed) and Priority.
    *   Add `Search` destination to `NavGraph`.

4.  **Integration** (Completed):
    *   Add entry point in `HomeScreen` (Magnifying glass icon).
