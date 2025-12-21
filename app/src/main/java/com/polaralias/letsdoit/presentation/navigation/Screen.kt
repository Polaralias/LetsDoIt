package com.polaralias.letsdoit.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TaskDetails : Screen("task_details/{taskId}?title={title}") {
        fun createRoute(taskId: String, title: String? = null) =
            if (title != null) "task_details/$taskId?title=$title" else "task_details/$taskId"
    }
    object Settings : Screen("settings")
    object Kanban : Screen("kanban")
    object ProjectList : Screen("project_list")
    object Insights : Screen("insights")
    object Search : Screen("search")
    object Calendar : Screen("calendar")
}
