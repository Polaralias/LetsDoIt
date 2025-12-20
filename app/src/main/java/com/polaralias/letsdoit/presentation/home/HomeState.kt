package com.polaralias.letsdoit.presentation.home

import com.polaralias.letsdoit.domain.ai.Suggestion
import com.polaralias.letsdoit.domain.model.Task

data class HomeState(
    val tasks: List<Task> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
