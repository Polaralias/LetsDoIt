package com.example.letsdoit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.letsdoit.ui.screens.ListsScreen
import com.example.letsdoit.ui.screens.TasksScreen

object Destinations {
    const val LISTS = "lists"
    const val TASKS = "tasks"
    const val TASKS_ROUTE = "$TASKS/{${Args.LIST_ID}}"

    object Args {
        const val LIST_ID = "listId"
    }

    fun tasks(listId: Long): String = "$TASKS/$listId"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.LISTS, modifier = modifier) {
        composable(Destinations.LISTS) {
            ListsScreen(onListSelected = { id ->
                navController.navigate(Destinations.tasks(id))
            })
        }
        composable(
            route = Destinations.TASKS_ROUTE,
            arguments = listOf(
                navArgument(Destinations.Args.LIST_ID) { type = NavType.LongType }
            )
        ) {
            TasksScreen()
        }
    }
}
