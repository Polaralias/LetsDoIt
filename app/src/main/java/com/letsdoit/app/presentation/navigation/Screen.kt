package com.letsdoit.app.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TaskDetails : Screen("task_details/{taskId}") {
        fun createRoute(taskId: String) = "task_details/$taskId"
    }
    object Settings : Screen("settings")
    object Kanban : Screen("kanban")
    object ProjectList : Screen("project_list")
}
