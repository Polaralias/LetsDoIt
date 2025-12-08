package com.letsdoit.app.presentation.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.domain.usecase.project.GetProjectsUseCase
import com.letsdoit.app.domain.usecase.project.GetSelectedProjectUseCase
import com.letsdoit.app.domain.usecase.project.SelectProjectUseCase
import com.letsdoit.app.domain.usecase.project.SyncProjectStructureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val getProjectsUseCase: GetProjectsUseCase,
    private val syncProjectStructureUseCase: SyncProjectStructureUseCase,
    private val selectProjectUseCase: SelectProjectUseCase,
    private val getSelectedProjectUseCase: GetSelectedProjectUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectListState())
    val state: StateFlow<ProjectListState> = _state.asStateFlow()

    init {
        getProjects()
        getSelectedProject()
        syncStructure()
    }

    private fun getProjects() {
        getProjectsUseCase()
            .onEach { projects ->
                _state.value = _state.value.copy(projects = projects)
            }
            .launchIn(viewModelScope)
    }

    private fun getSelectedProject() {
        getSelectedProjectUseCase()
            .onEach { selectedId ->
                _state.value = _state.value.copy(selectedProjectId = selectedId)
            }
            .launchIn(viewModelScope)
    }

    private fun syncStructure() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                syncProjectStructureUseCase()
                _state.value = _state.value.copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun onProjectSelect(projectId: String) {
        selectProjectUseCase(projectId)
    }
}
