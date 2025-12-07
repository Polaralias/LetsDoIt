package com.letsdoit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClickUpTaskDto(
    val id: String,
    val name: String,
    @SerializedName("text_content")
    val textContent: String?,
    val status: ClickUpStatusDto,
    @SerializedName("due_date")
    val dueDate: String?,
    val priority: ClickUpPriorityDto?,
    @SerializedName("date_created")
    val dateCreated: String,
    @SerializedName("date_updated")
    val dateUpdated: String,
    @SerializedName("list")
    val list: ClickUpListLightDto
)

data class ClickUpStatusDto(
    val status: String,
    val color: String,
    val type: String
)

data class ClickUpPriorityDto(
    val id: String?,
    val priority: String?,
    val color: String?,
    val orderindex: String?
)

data class ClickUpListLightDto(
    val id: String
)

data class ClickUpTasksResponse(
    val tasks: List<ClickUpTaskDto>
)

data class ClickUpCreateTaskRequest(
    val name: String,
    val description: String?,
    val status: String?,
    val priority: Int?,
    @SerializedName("due_date")
    val dueDate: Long?
)

data class ClickUpUpdateTaskRequest(
    val name: String?,
    val description: String?,
    val status: String?,
    val priority: Int?,
    @SerializedName("due_date")
    val dueDate: Long?
)
