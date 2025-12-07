package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        if (task.title.isBlank()) {
            throw IllegalArgumentException("Title cannot be empty")
        }
        repository.createTask(task)
    }
}
