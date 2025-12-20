package com.polaralias.letsdoit.domain.usecase.project

import com.polaralias.letsdoit.domain.repository.ProjectRepository
import javax.inject.Inject

class SyncProjectStructureUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke() {
        repository.syncStructure()
    }
}
