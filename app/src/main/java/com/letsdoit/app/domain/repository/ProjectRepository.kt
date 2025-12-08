package com.letsdoit.app.domain.repository

import com.letsdoit.app.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    suspend fun syncStructure()
}
