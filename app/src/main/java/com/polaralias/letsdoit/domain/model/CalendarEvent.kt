package com.polaralias.letsdoit.domain.model

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val start: Long,
    val end: Long,
    val color: Int,
    val allDay: Boolean
)
