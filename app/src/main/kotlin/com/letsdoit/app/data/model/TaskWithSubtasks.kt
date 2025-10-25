package com.letsdoit.app.data.model

data class TaskWithSubtasks(
    val task: Task,
    val subtasks: List<Subtask>
)
