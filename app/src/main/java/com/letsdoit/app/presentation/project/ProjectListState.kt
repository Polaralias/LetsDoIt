package com.letsdoit.app.presentation.project

import com.letsdoit.app.domain.model.Project

data class ProjectListState(
    val projects: List<Project> = emptyList(),
    val selectedProjectId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
