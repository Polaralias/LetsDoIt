package com.polaralias.letsdoit.presentation.kanban

import com.polaralias.letsdoit.domain.model.Task

enum class KanbanColumn(val title: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done")
}

data class KanbanState(
    val columns: Map<KanbanColumn, List<Task>> = mapOf(
        KanbanColumn.TODO to emptyList(),
        KanbanColumn.IN_PROGRESS to emptyList(),
        KanbanColumn.DONE to emptyList()
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)
