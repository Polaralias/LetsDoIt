package com.letsdoit.app.domain.usecase.project

import com.letsdoit.app.domain.model.Project
import com.letsdoit.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    operator fun invoke(): Flow<List<Project>> {
        return repository.getProjects()
    }
}
