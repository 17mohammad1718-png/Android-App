package com.dataguard.app.presentation.navigation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dataguard.app.R
import com.dataguard.app.presentation.screens.applist.AppListScreen
import com.dataguard.app.presentation.screens.dashboard.DashboardScreen
import com.dataguard.app.presentation.screens.datacap.DataCapScreen
import com.dataguard.app.presentation.screens.history.HistoryScreen
import com.dataguard.app.presentation.screens.onboarding.OnboardingScreen
import com.dataguard.app.presentation.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object History : Screen("history")
    data object DataCap : Screen("data_cap")
    data object Settings : Screen("settings")
}

/**
 * Persists the onboarding-completed flag so the user doesn't see it again
 * after process death or app restarts.
 */
object OnboardingPrefs {
    private const val PREFS = "onboarding_prefs"
    private const val KEY_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_COMPLETED, true).apply()
    }
}

private data class BottomNavItem(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, R.string.nav_dashboard, Icons.Filled.Home),
    BottomNavItem(Screen.AppList, R.string.nav_apps, Icons.AutoMirrored.Filled.List),
    BottomNavItem(Screen.History, R.string.nav_history, Icons.Filled.DateRange),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings),
)

private val bottomRoutes = bottomNavItems.map { it.screen.route }.toSet()

@StringRes
private fun titleRes(route: String?): Int = when (route) {
    Screen.Dashboard.route -> R.string.nav_dashboard
    Screen.AppList.route -> R.string.nav_apps
    Screen.History.route -> R.string.nav_history
    Screen.DataCap.route -> R.string.cap_title
    Screen.Settings.route -> R.string.nav_settings
    else -> R.string.app_name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Skip onboarding if already completed.
    val startDest = if (OnboardingPrefs.isCompleted(context)) {
        Screen.Dashboard.route
    } else {
        Screen.Onboarding.route
    }

    Scaffold(
        topBar = {
            if (currentRoute != Screen.Onboarding.route) {
                TopAppBar(
                    title = { Text(stringResource(titleRes(currentRoute))) },
                    navigationIcon = {
                        if (currentRoute == Screen.DataCap.route) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.common_back),
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGranted = {
                        OnboardingPrefs.markCompleted(context)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onOpenCap = { navController.navigate(Screen.DataCap.route) },
                )
            }
            composable(Screen.AppList.route) {
                AppListScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.DataCap.route) {
                DataCapScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
