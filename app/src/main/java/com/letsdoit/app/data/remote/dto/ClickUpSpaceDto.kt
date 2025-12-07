package com.letsdoit.app.data.remote.dto

data class ClickUpSpaceDto(
    val id: String,
    val name: String,
    val private: Boolean?,
    val statuses: List<ClickUpStatusDto>?
)

data class ClickUpSpacesResponse(
    val spaces: List<ClickUpSpaceDto>
)

data class ClickUpSpaceLightDto(
    val id: String,
    val name: String?,
    val access: Boolean?
)
