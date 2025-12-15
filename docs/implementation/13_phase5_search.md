# Implementation Phase 13: Phase 5 Search

This document details the features for Phase 5: Search & Filtering.

## Goal
Enable users to quickly find tasks by title, description, or other criteria.

## Features

### 1. Search Screen
> **Goal**: A dedicated screen or bar to search tasks.
>
> *   **Search Bar**: Input text to filter tasks.
> *   **Results List**: Real-time filtering of tasks.
> *   **Filter Options** (Optional for now): Filter by Status, Priority.

## Implementation Steps

1.  **Data Layer**:
    *   Update `TaskDao` with a search query using `LIKE`.

2.  **Domain Layer**:
    *   Create `SearchTasksUseCase`.

3.  **Presentation Layer**:
    *   Create `SearchViewModel`.
    *   Create `SearchScreen` with `SearchBar` (Material 3).
    *   Add `Search` destination to `NavGraph`.

4.  **Integration**:
    *   Add entry point in `HomeScreen` (Magnifying glass icon).
