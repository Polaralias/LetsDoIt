package com.polaralias.letsdoit.data.remote.dto

data class ClickUpFolderDto(
    val id: String,
    val name: String,
    val orderindex: Int?,
    val override_statuses: Boolean?,
    val hidden: Boolean?,
    val space: ClickUpSpaceLightDto?,
    val task_count: String?,
    val lists: List<ClickUpListDto>?
)

data class ClickUpFolderLightDto(
    val id: String,
    val name: String?,
    val hidden: Boolean?,
    val access: Boolean?
)

data class ClickUpFoldersResponse(
    val folders: List<ClickUpFolderDto>
)
