package com.polaralias.letsdoit.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClickUpListDto(
    val id: String,
    val name: String,
    val orderindex: Int?,
    val content: String?,
    val status: ClickUpStatusDto?,
    val priority: ClickUpPriorityDto?,
    val assignee: String?,
    @SerializedName("task_count")
    val taskCount: Int?,
    @SerializedName("due_date")
    val dueDate: String?,
    @SerializedName("start_date")
    val startDate: String?,
    val folder: ClickUpFolderLightDto?,
    val space: ClickUpSpaceLightDto?,
    val archived: Boolean?,
    @SerializedName("override_statuses")
    val overrideStatuses: Boolean?,
    @SerializedName("permission_level")
    val permissionLevel: String?
)

data class ClickUpListsResponse(
    val lists: List<ClickUpListDto>
)
