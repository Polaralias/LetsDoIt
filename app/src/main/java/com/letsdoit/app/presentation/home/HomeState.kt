package com.letsdoit.app.presentation.home

import com.letsdoit.app.domain.ai.Suggestion
import com.letsdoit.app.domain.model.Task

data class HomeState(
    val tasks: List<Task> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
