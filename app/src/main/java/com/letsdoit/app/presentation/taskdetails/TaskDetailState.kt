package com.letsdoit.app.presentation.taskdetails

import com.letsdoit.app.domain.model.Task

data class TaskDetailState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val isSaved: Boolean = false
)
