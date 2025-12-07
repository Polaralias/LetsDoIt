package com.letsdoit.app.domain.model

data class TaskQueue(
    val id: String,
    val name: String,
    val tasks: List<Task>
)
