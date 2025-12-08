package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(listId: String? = null): Flow<List<Task>> {
        return repository.getTasksFlow(listId)
    }
}
