package com.letsdoit.app.domain.usecase.project

import com.letsdoit.app.domain.repository.ProjectRepository
import javax.inject.Inject

class SyncProjectStructureUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke() {
        repository.syncStructure()
    }
}
