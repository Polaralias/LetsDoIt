package com.polaralias.letsdoit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BucketsViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    val lists: StateFlow<List<ListEntity>> = taskRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            taskRepository.ensureDefaultList()
        }
    }
}
