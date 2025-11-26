package com.example.letsdoit.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.letsdoit.data.ListEntity
import com.example.letsdoit.data.ListRepository
import com.example.letsdoit.data.TaskEntity
import com.example.letsdoit.data.TaskPriority
import com.example.letsdoit.data.TaskRepository
import com.example.letsdoit.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val listId: Long = checkNotNull(savedStateHandle.get<Long>(Destinations.Args.LIST_ID))

    private val newTaskTitle = MutableStateFlow("")
    private val newTaskNotes = MutableStateFlow("")

    private val list = listRepository.getList(listId)
    private val tasks = taskRepository.getTasksForList(listId)

    val uiState: StateFlow<TasksUiState> = combine(list, tasks, newTaskTitle, newTaskNotes) { list, tasks, title, notes ->
        TasksUiState(
            listId = listId,
            list = list,
            tasks = tasks,
            newTaskTitle = title,
            newTaskNotes = notes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState(listId = listId))

    fun onNewTaskTitleChange(value: String) {
        newTaskTitle.value = value
    }

    fun onNewTaskNotesChange(value: String) {
        newTaskNotes.value = value
    }

    fun addTask() {
        val title = newTaskTitle.value.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.createTask(
                listId = listId,
                title = title,
                notes = newTaskNotes.value.trim().ifBlank { null },
                dueAt = null,
                priority = TaskPriority.NONE
            )
            newTaskTitle.value = ""
            newTaskNotes.value = ""
        }
    }

    fun toggleCompletion(id: Long) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(id)
        }
    }

    fun updateTask(task: TaskEntity, title: String, notes: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return
        viewModelScope.launch {
            taskRepository.updateTask(
                id = task.id,
                listId = task.listId,
                title = trimmedTitle,
                notes = notes.trim().ifBlank { null },
                dueAt = task.dueAt,
                priority = task.priority
            )
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(id)
        }
    }
}

data class TasksUiState(
    val listId: Long,
    val list: ListEntity? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val newTaskTitle: String = "",
    val newTaskNotes: String = ""
)
