package com.polaralias.letsdoit.domain.usecase.project

import com.polaralias.letsdoit.domain.model.Project
import com.polaralias.letsdoit.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    operator fun invoke(): Flow<List<Project>> {
        return repository.getProjects()
    }
}
