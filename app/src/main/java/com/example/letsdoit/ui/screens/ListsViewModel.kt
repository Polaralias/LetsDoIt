package com.example.letsdoit.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.letsdoit.data.ListEntity
import com.example.letsdoit.data.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ListsViewModel @Inject constructor(private val listRepository: ListRepository) : ViewModel() {
    private val newListName = MutableStateFlow("")

    private val lists = listRepository.getLists()

    val uiState: StateFlow<ListsUiState> = combine(lists, newListName) { lists, name ->
        ListsUiState(lists = lists, newListName = name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListsUiState())

    fun onNewListNameChange(value: String) {
        newListName.value = value
    }

    fun createList() {
        val name = newListName.value.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            listRepository.createList(name)
            newListName.value = ""
        }
    }

    fun renameList(id: Long, name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            listRepository.renameList(id, trimmedName)
        }
    }

    fun deleteList(id: Long) {
        viewModelScope.launch {
            listRepository.deleteList(id)
        }
    }
}

data class ListsUiState(
    val lists: List<ListEntity> = emptyList(),
    val newListName: String = ""
)
