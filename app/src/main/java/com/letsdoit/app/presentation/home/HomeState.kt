package com.letsdoit.app.presentation.home

import com.letsdoit.app.domain.model.Task

data class HomeState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
