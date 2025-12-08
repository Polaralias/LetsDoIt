package com.letsdoit.app.domain.usecase.project

import com.letsdoit.app.domain.repository.PreferencesRepository
import javax.inject.Inject

class SelectProjectUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(projectId: String) {
        preferencesRepository.setSelectedListId(projectId)
    }
}
