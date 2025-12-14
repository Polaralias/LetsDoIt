# Implementation Phase 12: Phase 4 Insights

This document details the features for Phase 4: Insights & Analytics.

## Goal
Provide users with productivity analytics and insights into their task management habits.

## Features

### 1. Insights Dashboard
> **Goal**: A dedicated screen to visualize task statistics.
>
> *   **Overview Cards**:
>     *   Total Active Tasks
>     *   Total Completed Tasks (All time or filtered)
> *   **Charts**:
>     *   **Tasks by Priority**: Pie chart showing distribution of priorities (Urgent, High, Normal, Low).
>     *   **Tasks by Status**: Pie chart showing distribution of statuses (To Do, In Progress, Done).
> *   **Navigation**: Accessible from the Home Screen (Top Bar or Menu).

## Implementation Steps

1.  **Data Layer**:
    *   Update `TaskDao` to return aggregated data (counts).
    *   Create `InsightsRepository`.

2.  **Domain Layer**:
    *   Create `GetInsightsUseCase` to fetch and combine statistics.
    *   Define `InsightsData` model.

3.  **Presentation Layer**:
    *   Create `InsightsViewModel`.
    *   Create `InsightsScreen` using Jetpack Compose.
    *   Use Canvas or simple Boxes for "Charts" if no charting library is available (to keep dependencies low).

4.  **Integration**:
    *   Add `Insights` destination to `NavGraph`.
    *   Add entry point in `HomeScreen`.

## Future Considerations
*   **Weekly Trends**: Bar chart of tasks completed per day.
*   **Time Tracking**: If we add "Time Spent" to tasks.
