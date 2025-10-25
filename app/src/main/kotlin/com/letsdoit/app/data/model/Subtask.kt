package com.letsdoit.app.data.model

data class Subtask(
    val id: Long,
    val parentTaskId: Long,
    val title: String,
    val done: Boolean,
    val dueAt: Long?,
    val orderInParent: Int,
    val startAt: Long?,
    val durationMinutes: Int?
)
