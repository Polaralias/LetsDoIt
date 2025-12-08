package com.letsdoit.app.domain.usecase.project

import com.letsdoit.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSelectedProjectUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<String?> {
        return preferencesRepository.getSelectedListIdFlow()
    }

    fun getSync(): String? {
        return preferencesRepository.getSelectedListId()
    }
}
