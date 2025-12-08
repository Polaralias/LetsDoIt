package com.letsdoit.app.data.remote.dto

data class ClickUpTeamDto(
    val id: String,
    val name: String,
    val color: String?,
    val avatar: String?
)

data class ClickUpTeamsResponse(
    val teams: List<ClickUpTeamDto>
)
