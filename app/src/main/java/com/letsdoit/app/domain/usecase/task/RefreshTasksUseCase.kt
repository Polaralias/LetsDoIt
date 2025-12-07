package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.repository.TaskRepository
import javax.inject.Inject

class RefreshTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(listId: String) {
        repository.refreshTasks(listId)
    }
}
