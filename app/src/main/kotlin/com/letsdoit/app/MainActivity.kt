package com.letsdoit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.letsdoit.app.ui.components.AppBottomBar
import com.letsdoit.app.ui.components.AppDestination
import com.letsdoit.app.ui.components.AppTopBar
import com.letsdoit.app.ui.screens.BucketsScreen
import com.letsdoit.app.ui.screens.BulkAddScreen
import com.letsdoit.app.ui.screens.SettingsScreen
import com.letsdoit.app.ui.screens.TasksListScreen
import com.letsdoit.app.ui.screens.TimelineScreen
import com.letsdoit.app.ui.theme.AppTheme
import com.letsdoit.app.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.theme.collectAsState()
            val navController = rememberNavController()
            val destinations = listOf(
                AppDestination(Destinations.List.route, stringResource(id = R.string.nav_list), Icons.Filled.List),
                AppDestination(Destinations.Timeline.route, stringResource(id = R.string.nav_timeline), Icons.Filled.CalendarMonth),
                AppDestination(Destinations.Buckets.route, stringResource(id = R.string.nav_buckets), Icons.Outlined.AutoAwesomeMosaic),
                AppDestination(Destinations.Settings.route, stringResource(id = R.string.nav_settings), Icons.Filled.Settings)
            )
            AppTheme(config = theme) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: Destinations.List.route
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        val title = when (currentRoute) {
                            Destinations.BulkAdd.route -> stringResource(id = R.string.bulk_title)
                            else -> stringResource(id = R.string.app_name)
                        }
                        AppTopBar(
                            title = title,
                            actions = {
                                if (currentRoute == Destinations.List.route || currentRoute == Destinations.Buckets.route) {
                                    TextButton(onClick = {
                                        navController.navigate(Destinations.BulkAdd.route)
                                    }) {
                                        Text(text = stringResource(id = R.string.bulk_title))
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        AppBottomBar(
                            destinations = destinations,
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (route != currentRoute) {
                                    navController.navigate(route) {
                                        popUpTo(Destinations.List.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                ) { padding ->
                    AppNavGraph(padding, navController)
                }
            }
        }
    }
}

@Composable
private fun AppNavGraph(padding: PaddingValues, navController: androidx.navigation.NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.List.route,
        modifier = Modifier.padding(padding)
    ) {
        composable(Destinations.List.route) {
            TasksListScreen(onOpenSettings = { navController.navigate(Destinations.Settings.route) })
        }
        composable(Destinations.Timeline.route) {
            TimelineScreen()
        }
        composable(Destinations.Buckets.route) {
            BucketsScreen()
        }
        composable(Destinations.Settings.route) {
            SettingsScreen()
        }
        composable(Destinations.BulkAdd.route) {
            BulkAddScreen()
        }
    }
}

sealed class Destinations(val route: String) {
    data object List : Destinations("list")
    data object Timeline : Destinations("timeline")
    data object Buckets : Destinations("buckets")
    data object Settings : Destinations("settings")
    data object BulkAdd : Destinations("bulkAdd")

    companion object {
        val all = listOf(List, Timeline, Buckets, Settings)
    }
}
