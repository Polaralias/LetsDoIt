package com.letsdoit.app.data.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubtasksRelation(
    @Embedded val task: TaskEntity,
    @Relation(parentColumn = "id", entityColumn = "parentTaskId")
    val subtasks: List<SubtaskEntity>
)
