package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskStatusUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val updateTaskUseCase: UpdateTaskUseCase
) {
    suspend operator fun invoke(taskId: String) {
        val task = repository.getTask(taskId) ?: return
        val newStatus = if (task.status == "complete") "open" else "complete"
        updateTaskUseCase(task.copy(status = newStatus))
    }
}
