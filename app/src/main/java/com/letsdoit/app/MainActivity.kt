package com.letsdoit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.letsdoit.app.presentation.home.HomeScreen
import com.letsdoit.app.presentation.kanban.KanbanScreen
import com.letsdoit.app.presentation.project.ProjectListScreen
import com.letsdoit.app.presentation.settings.SettingsScreen
import com.letsdoit.app.presentation.taskdetails.TaskDetailScreen
import com.letsdoit.app.presentation.navigation.Screen
import com.letsdoit.app.presentation.theme.LetsDoItTheme
import com.letsdoit.app.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeState by themeViewModel.themeState.collectAsState()

            LetsDoItTheme(
                themeMode = themeState.themeMode,
                dynamicColor = themeState.isDynamicColorEnabled
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onAddTaskClick = {
                                    navController.navigate(Screen.TaskDetails.createRoute("new"))
                                },
                                onTaskClick = { taskId ->
                                    navController.navigate(Screen.TaskDetails.createRoute(taskId))
                                },
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onKanbanClick = {
                                    navController.navigate(Screen.Kanban.route)
                                },
                                onProjectListClick = {
                                    navController.navigate(Screen.ProjectList.route)
                                }
                            )
                        }
                        composable(Screen.Kanban.route) {
                            KanbanScreen(navController = navController)
                        }
                        composable(Screen.TaskDetails.route) {
                            TaskDetailScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                        composable(Screen.ProjectList.route) {
                            ProjectListScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
