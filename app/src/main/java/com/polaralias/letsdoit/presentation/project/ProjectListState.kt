package com.polaralias.letsdoit.presentation.project

import com.polaralias.letsdoit.domain.model.Project

data class ProjectListState(
    val projects: List<Project> = emptyList(),
    val selectedProjectId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
