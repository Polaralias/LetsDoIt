package com.polaralias.letsdoit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.polaralias.letsdoit.navigation.AppIntentExtras
import com.polaralias.letsdoit.navigation.NavStateKeys
import com.polaralias.letsdoit.ui.components.AppBottomBar
import com.polaralias.letsdoit.ui.components.AppDestination
import com.polaralias.letsdoit.ui.components.AppTopBar
import com.polaralias.letsdoit.ui.components.SearchBarState
import com.polaralias.letsdoit.ui.screens.BucketsScreen
import com.polaralias.letsdoit.ui.screens.BulkAddScreen
import com.polaralias.letsdoit.ui.screens.JoinScreen
import com.polaralias.letsdoit.ui.screens.ShareScreen
import com.polaralias.letsdoit.ui.screens.SearchScreen
import com.polaralias.letsdoit.ui.screens.SettingsScreen
import com.polaralias.letsdoit.ui.screens.TasksListScreen
import com.polaralias.letsdoit.ui.screens.TimelineScreen
import com.polaralias.letsdoit.ui.theme.AppTheme
import com.polaralias.letsdoit.ui.viewmodel.MainViewModel
import com.polaralias.letsdoit.ui.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.navigation.NavHostController
import androidx.navigation.navArgument

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val deepLinkEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val timelineFocusEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    private val bulkAddRequests = MutableSharedFlow<String?>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        val fontScaleOverride = intent?.getFloatExtra(AppIntentExtras.FONT_SCALE_OVERRIDE, 0f)
            ?.takeIf { it >= 0.5f }
        setContent {
            val theme by viewModel.theme.collectAsState()
            val searchState by searchViewModel.uiState.collectAsState()
            val navController = rememberNavController()
            val destinations = listOf(
                AppDestination(Destinations.List.route, stringResource(id = R.string.nav_list), Icons.Filled.List),
                AppDestination(Destinations.Timeline.route, stringResource(id = R.string.nav_timeline), Icons.Filled.CalendarMonth),
                AppDestination(Destinations.Buckets.route, stringResource(id = R.string.nav_buckets), Icons.Outlined.AutoAwesomeMosaic),
                AppDestination(Destinations.Settings.route, stringResource(id = R.string.nav_settings), Icons.Filled.Settings)
            )
            AppTheme(config = theme, fontScaleOverride = fontScaleOverride) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: Destinations.List.route
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        val title = when (currentRoute) {
                            Destinations.BulkAdd.route -> stringResource(id = R.string.bulk_title)
                            else -> stringResource(id = R.string.app_name)
                        }
                        val searchBarState = SearchBarState(
                            query = searchState.query,
                            history = searchState.history,
                            active = searchState.searchActive,
                            onQueryChange = searchViewModel::onQueryChange,
                            onSearch = {
                                searchViewModel.submitQuery()
                                navController.navigate(Destinations.Search.route) { launchSingleTop = true }
                            },
                            onActiveChange = searchViewModel::onSearchActiveChange,
                            onSelectHistory = {
                                searchViewModel.selectHistory(it)
                                navController.navigate(Destinations.Search.route) { launchSingleTop = true }
                            }
                        )
                        val navigationIcon: (@Composable () -> Unit)? = if (currentRoute == Destinations.Search.route) {
                            {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
                                }
                            }
                        } else {
                            null
                        }
                        AppTopBar(
                            title = title,
                            searchState = searchBarState,
                            navigation = navigationIcon,
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
                    AppNavGraph(padding, navController, deepLinkEvents, timelineFocusEvents, bulkAddRequests)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.dataString?.let { deepLinkEvents.tryEmit(it) }
        if (intent?.getBooleanExtra(AppIntentExtras.OPEN_BULK_FROM_WIDGET, false) == true) {
            val seed = intent.getStringExtra(AppIntentExtras.CLIPBOARD_SEED)
            bulkAddRequests.tryEmit(seed)
        }
        intent?.getLongExtra(AppIntentExtras.TIMELINE_TASK_ID, 0L)?.takeIf { it != 0L }?.let { target ->
            timelineFocusEvents.tryEmit(target)
        }
    }
}

@Composable
private fun AppNavGraph(
    padding: PaddingValues,
    navController: NavHostController,
    deepLinks: SharedFlow<String>,
    timelineFocus: SharedFlow<Long>,
    bulkAddRequests: SharedFlow<String?>
) {
    androidx.compose.runtime.LaunchedEffect(deepLinks) {
        deepLinks.collect { link ->
            navController.navigate(Destinations.Join.createRoute(link)) {
                launchSingleTop = true
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(timelineFocus) {
        timelineFocus.collect { taskId ->
            navController.navigate(Destinations.Timeline.route) {
                popUpTo(Destinations.List.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            val entry = navController.getBackStackEntry(Destinations.Timeline.route)
            entry.savedStateHandle[NavStateKeys.TIMELINE_FOCUS] = taskId
        }
    }
    androidx.compose.runtime.LaunchedEffect(bulkAddRequests) {
        bulkAddRequests.collect { seed ->
            navController.navigate(Destinations.BulkAdd.route) {
                popUpTo(Destinations.List.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            val entry = navController.getBackStackEntry(Destinations.BulkAdd.route)
            entry.savedStateHandle[NavStateKeys.BULK_SEED] = seed.orEmpty()
        }
    }
    NavHost(
        navController = navController,
        startDestination = Destinations.List.route,
        modifier = Modifier.padding(padding)
    ) {
        composable(Destinations.List.route) {
            TasksListScreen(onOpenSettings = { navController.navigate(Destinations.Settings.route) })
        }
        composable(Destinations.Timeline.route) { entry ->
            TimelineScreen(entry = entry)
        }
        composable(Destinations.Buckets.route) {
            BucketsScreen()
        }
        composable(Destinations.Settings.route) {
            SettingsScreen(
                onOpenShare = { navController.navigate(Destinations.Share.route) },
                onOpenJoin = { navController.navigate(Destinations.Join.createRoute(null)) }
            )
        }
        composable(Destinations.BulkAdd.route) { entry ->
            BulkAddScreen(entry = entry)
        }
        composable(Destinations.Share.route) {
            ShareScreen(onOpenJoin = { navController.navigate(Destinations.Join.createRoute(null)) })
        }
        composable(Destinations.Search.route) {
            SearchScreen(viewModel = searchViewModel)
        }
        composable(
            route = Destinations.Join.route,
            arguments = listOf(navArgument(Destinations.Join.linkArg) { nullable = true; defaultValue = null })
        ) { entry ->
            val link = entry.arguments?.getString(Destinations.Join.linkArg)
            JoinScreen(initialLink = link)
        }
    }
}

sealed class Destinations(val route: String) {
    data object List : Destinations("list")
    data object Timeline : Destinations("timeline")
    data object Buckets : Destinations("buckets")
    data object Settings : Destinations("settings")
    data object BulkAdd : Destinations("bulkAdd")
    data object Share : Destinations("share")
    data object Search : Destinations("search")
    data object Join : Destinations("join?link={link}") {
        const val linkArg = "link"
        fun createRoute(link: String?): String {
            return if (link.isNullOrBlank()) {
                "join"
            } else {
                "join?link=${Uri.encode(link)}"
            }
        }
    }

    companion object {
        val all = listOf(List, Timeline, Buckets, Settings)
    }
}
