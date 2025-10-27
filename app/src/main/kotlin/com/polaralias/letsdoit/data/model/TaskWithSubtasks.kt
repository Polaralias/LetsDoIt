package com.polaralias.letsdoit.data.model

data class TaskWithSubtasks(
    val task: Task,
    val subtasks: List<Subtask>
)
