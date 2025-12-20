package com.polaralias.letsdoit.presentation.taskdetails

import com.polaralias.letsdoit.domain.model.Task
import java.time.LocalDateTime

data class TaskDetailState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val isSaved: Boolean = false,
    val suggestedDueDate: LocalDateTime? = null,
    val suggestedPriority: Int? = null,
    val suggestedRecurrence: String? = null
)
