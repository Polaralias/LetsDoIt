package com.polaralias.letsdoit.data.search

import com.polaralias.letsdoit.data.model.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

enum class SmartFilterKind {
    DueToday,
    Overdue,
    NoDueDate,
    HighPriority,
    LinkedToClickUp,
    Shared
}

interface SearchRepository {
    fun search(query: String): Flow<List<TaskWithSubtasks>>
    fun smartFilter(kind: SmartFilterKind): Flow<List<TaskWithSubtasks>>
    val history: Flow<List<String>>
    suspend fun recordQuery(query: String)
    suspend fun removeQuery(query: String)
}
