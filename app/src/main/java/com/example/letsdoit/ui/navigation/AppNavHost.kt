package com.example.letsdoit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.letsdoit.ui.screens.ListsScreen

object Destinations {
    const val LISTS = "lists"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.LISTS, modifier = modifier) {
        composable(Destinations.LISTS) {
            ListsScreen()
        }
    }
}
