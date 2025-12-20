package com.polaralias.letsdoit.domain.model

data class TaskQueue(
    val id: String,
    val name: String,
    val tasks: List<Task>
)
