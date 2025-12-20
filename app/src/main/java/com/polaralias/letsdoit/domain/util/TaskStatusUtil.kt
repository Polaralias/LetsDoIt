package com.polaralias.letsdoit.domain.util

object TaskStatusUtil {
    const val OPEN = "Open"
    const val COMPLETED = "Completed"
    const val IN_PROGRESS = "In Progress"

    val COMPLETED_STATUSES = listOf("Completed", "Done", "complete", "closed")
    val TODO_STATUSES = listOf("Open", "To Do", "todo", "", "open")
    val IN_PROGRESS_STATUSES = listOf("In Progress", "doing")

    fun isCompleted(status: String): Boolean {
        return COMPLETED_STATUSES.any { it.equals(status, ignoreCase = true) }
    }

    fun isTodo(status: String): Boolean {
        return TODO_STATUSES.any { it.equals(status, ignoreCase = true) }
    }

    fun isInProgress(status: String): Boolean {
        return IN_PROGRESS_STATUSES.any { it.equals(status, ignoreCase = true) }
    }
}
