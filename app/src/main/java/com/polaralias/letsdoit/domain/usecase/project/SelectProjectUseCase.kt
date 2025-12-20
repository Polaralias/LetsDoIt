package com.polaralias.letsdoit.domain.usecase.project

import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import javax.inject.Inject

class SelectProjectUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(projectId: String) {
        preferencesRepository.setSelectedListId(projectId)
    }
}
