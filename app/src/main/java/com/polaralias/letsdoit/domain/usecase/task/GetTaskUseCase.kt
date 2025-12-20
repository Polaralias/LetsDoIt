package com.polaralias.letsdoit.domain.usecase.task

import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: String): Task? {
        return repository.getTask(id)
    }
}
