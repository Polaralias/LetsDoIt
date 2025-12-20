package com.polaralias.letsdoit.domain.repository

import com.polaralias.letsdoit.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    suspend fun syncStructure()
}
