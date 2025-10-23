package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TimelineViewModel @Inject constructor(
    taskRepository: TaskRepository
) : ViewModel() {
    val tasks: StateFlow<List<Task>> = taskRepository.observeTimeline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
