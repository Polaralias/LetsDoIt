# Implementation Phase 6: NLP Engine

This document details the implementation of the Natural Language Processing engine for task input.

## Goal
Allow users to type tasks like "Buy milk tomorrow at 5pm" and automatically extract the title and due date.

## Prompts

### 1. Create NLP Service
> Create `domain/nlp/NlpEngine.kt`:
> *   Function `parse(input: String): NlpResult`.
> *   `data class NlpResult(val cleanTitle: String, val detectedDate: LocalDateTime?, val detectedPriority: Priority?)`.

### 2. Implement Date Parsing Logic
> Implement logic to detect patterns:
> *   "Tomorrow", "Today", "Yesterday".
> *   "Next [Day of Week]".
> *   "in X minutes/hours/days".
> *   "at [Time]".
> *   (Consider using a library like `natty` (Java) or writing specific Regex patterns if lightweight is preferred).
> *   *Note*: Ensure timezone handling is correct.

### 3. Integrate with ViewModel
> Update `TaskDetailViewModel` (or `CreateTaskViewModel`):
> *   Observe the title input field.
> *   Debounce the input.
> *   Call `NlpEngine.parse(text)`.
> *   Update UI state to show "suggested date" based on parsing.
> *   On save, apply the detected date if the user hasn't manually overridden it.

### 4. Verification
> Write unit tests for `NlpEngine`:
> *   Input: "Meeting next Friday at 10am" -> Verify date is correct Friday 10:00.
> *   Input: "Do laundry in 2 hours" -> Verify date is roughly now + 2h.
