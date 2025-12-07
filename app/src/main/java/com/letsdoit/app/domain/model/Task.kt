package com.letsdoit.app.domain.model

import java.time.LocalDateTime

data class Task(
    val id: String,
    val listId: String,
    val title: String,
    val description: String?,
    val status: String,
    val dueDate: LocalDateTime?,
    val priority: Int,
    val isSynced: Boolean = true,
    val calendarEventId: Long? = null
)
