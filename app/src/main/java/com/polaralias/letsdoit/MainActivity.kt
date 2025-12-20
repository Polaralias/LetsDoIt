package com.polaralias.letsdoit

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
import com.polaralias.letsdoit.presentation.home.HomeScreen
import com.polaralias.letsdoit.presentation.insights.InsightsScreen
import com.polaralias.letsdoit.presentation.kanban.KanbanScreen
import com.polaralias.letsdoit.presentation.project.ProjectListScreen
import com.polaralias.letsdoit.presentation.search.SearchScreen
import com.polaralias.letsdoit.presentation.settings.SettingsScreen
import com.polaralias.letsdoit.presentation.taskdetails.TaskDetailScreen
import com.polaralias.letsdoit.presentation.navigation.Screen
import com.polaralias.letsdoit.presentation.theme.LetsDoItTheme
import com.polaralias.letsdoit.presentation.theme.ThemeViewModel
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
                themeColor = themeState.themeColor,
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
                                },
                                onInsightsClick = {
                                    navController.navigate(Screen.Insights.route)
                                },
                                onSearchClick = {
                                    navController.navigate(Screen.Search.route)
                                },
                                onSuggestionClick = { title ->
                                    // Navigate to create new task with pre-filled title
                                    // We need to pass the title to TaskDetailScreen
                                    // For now, let's just create a "new" task and maybe we can pass arguments later
                                    // or just navigate to "new" and let the user type.
                                    // To do it properly, we should pass title as a nav argument or share via ViewModel

                                    // Since Screen.TaskDetails.createRoute takes an ID, we can't easily pass the title in the ID.
                                    // However, we can encode it in the URL if we change the route definition, or use a shared ViewModel.
                                    // Or we can add an optional query parameter?

                                    // Let's assume for now we just open the "new" screen.
                                    // Ideally we should pre-fill.
                                    // I'll append a query parameter ?title=...

                                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                    navController.navigate("task_details/new?title=$encodedTitle")
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
                        composable(Screen.Insights.route) {
                            InsightsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.ProjectList.route) {
                            ProjectListScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.Search.route) {
                            SearchScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onTaskClick = { taskId ->
                                    navController.navigate(Screen.TaskDetails.createRoute(taskId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
