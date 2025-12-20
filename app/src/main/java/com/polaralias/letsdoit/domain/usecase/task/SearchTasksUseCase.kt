package com.polaralias.letsdoit.domain.usecase.task

import com.polaralias.letsdoit.domain.model.SearchFilter
import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(query: String, filter: SearchFilter = SearchFilter()): Flow<List<Task>> {
        return repository.searchTasks(query).map { tasks ->
            tasks.filter { task ->
                val statusMatch = if (filter.status.isEmpty()) true else {
                    filter.status.any { it.equals(task.status, ignoreCase = true) }
                }
                val priorityMatch = if (filter.priority.isEmpty()) true else {
                    filter.priority.contains(task.priority)
                }
                statusMatch && priorityMatch
            }
        }
    }
}
