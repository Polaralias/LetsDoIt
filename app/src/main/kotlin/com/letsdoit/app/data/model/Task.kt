package com.letsdoit.app.data.model

import java.time.Instant

data class Task(
    val id: Long,
    val listId: Long,
    val title: String,
    val notes: String?,
    val dueAt: Instant?,
    val repeatRule: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completed: Boolean,
    val priority: Int,
    val orderInList: Int,
    val startAt: Instant?,
    val durationMinutes: Int?,
    val calendarEventId: Long?,
    val column: String
)
