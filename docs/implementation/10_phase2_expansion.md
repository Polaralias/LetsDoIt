# Implementation Phase 10: Phase 2 Expansion

This document details the expansion features for Phase 2.

## Goal
Add visual workflow modeling (Kanban) and recurring task support.

## Prompts

### 1. Kanban Board
> **Goal**: Display tasks in columns by status.
>
> *   Create `presentation/kanban/KanbanScreen.kt`.
> *   Use a `HorizontalPager` or a horizontal `LazyRow` of `LazyColumn`s.
> *   Each column represents a status (e.g., "To Do", "In Progress", "Done").
> *   Implement Drag-and-Drop (using Compose DragAndDrop APIs or libraries) to move tasks between columns.
> *   Update task status in ViewModel on drop.

### 2. Recurring Tasks
> **Goal**: Allow tasks to repeat (e.g., "Every Monday").
>
> *   Update `Task` entity: Add `recurrenceRule` (String, e.g., RRule format "FREQ=WEEKLY;BYDAY=MO").
> *   Update `TaskRepository`: When completing a recurring task:
>     *   Mark current as done.
>     *   Create *new* task with the next due date calculated from the rule.
> *   UI: Add "Repeat" option in Task Details (None, Daily, Weekly, Custom).

### 3. Advanced NLP
> **Goal**: Parse recurring rules from text.
>
> *   Update `NlpEngine`: Detect "Every [Day]" or "Daily".
> *   Map to RRule format.
