package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class TimelineViewModel @Inject constructor(
    taskRepository: TaskRepository
) : ViewModel() {
    val tasks: Flow<PagingData<Task>> = taskRepository.observeTimeline().cachedIn(viewModelScope)
}
