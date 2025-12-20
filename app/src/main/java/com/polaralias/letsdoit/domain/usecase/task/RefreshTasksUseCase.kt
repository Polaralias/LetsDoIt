package com.polaralias.letsdoit.domain.usecase.task

import com.polaralias.letsdoit.domain.repository.TaskRepository
import javax.inject.Inject

class RefreshTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(listId: String) {
        repository.refreshTasks(listId)
    }
}
