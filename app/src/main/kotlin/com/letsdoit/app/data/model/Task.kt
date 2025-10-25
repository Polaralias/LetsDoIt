package com.letsdoit.app.data.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Task(
    val id: Long,
    val listId: Long,
    val title: String,
    val notes: String?,
    val dueAt: Instant?,
    val repeatRule: String?,
    val remindOffsetMinutes: Int?,
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
