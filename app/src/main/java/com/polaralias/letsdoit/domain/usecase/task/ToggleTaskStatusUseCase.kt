package com.polaralias.letsdoit.domain.usecase.task

import com.polaralias.letsdoit.domain.repository.TaskRepository
import com.polaralias.letsdoit.domain.util.TaskStatusUtil
import javax.inject.Inject

class ToggleTaskStatusUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val updateTaskUseCase: UpdateTaskUseCase
) {
    suspend operator fun invoke(taskId: String) {
        val task = repository.getTask(taskId) ?: return
        val newStatus = if (TaskStatusUtil.isCompleted(task.status)) {
            TaskStatusUtil.OPEN
        } else {
            TaskStatusUtil.COMPLETED
        }
        updateTaskUseCase(task.copy(status = newStatus))
    }
}
