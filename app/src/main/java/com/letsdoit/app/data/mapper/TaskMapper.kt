package com.letsdoit.app.data.mapper

import com.letsdoit.app.data.local.entity.TaskEntity
import com.letsdoit.app.data.remote.dto.ClickUpTaskDto
import com.letsdoit.app.domain.model.Task
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

fun LocalDateTime.toEpochMilli(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        listId = listId,
        title = title,
        description = description,
        status = status,
        dueDate = dueDate?.toLocalDateTime(),
        priority = priority,
        isSynced = isSynced
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        listId = listId,
        title = title,
        description = description,
        status = status,
        dueDate = dueDate?.toEpochMilli(),
        priority = priority,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isSynced = isSynced
    )
}

fun ClickUpTaskDto.toEntity(listId: String): TaskEntity {
    val dueDateLong = try {
        dueDate?.toLong()
    } catch (e: Exception) {
        null
    }

    return TaskEntity(
        id = id,
        listId = listId,
        title = name,
        description = textContent,
        status = status.status,
        dueDate = dueDateLong,
        priority = priority?.id?.toIntOrNull() ?: 0,
        createdAt = dateCreated.toLongOrNull() ?: System.currentTimeMillis(),
        updatedAt = dateUpdated.toLongOrNull() ?: System.currentTimeMillis(),
        isSynced = true
    )
}
