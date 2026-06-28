package com.example.stocktracker.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stocktracker.ui.detail.DetailScreen
import com.example.stocktracker.ui.search.SearchScreen
import com.example.stocktracker.ui.watchlist.WatchlistScreen

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isOnDetailScreen = currentDestination?.route?.startsWith("detail/") == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = !isOnDetailScreen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                MidasBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.WATCHLIST.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(
                route = AppDestination.WATCHLIST.route,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(220)) },
            ) {
                WatchlistScreen(
                    onStockClick = { navController.navigate(detailRoute(it)) },
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                )
            }

            composable(
                route = AppDestination.SEARCH.route,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(220)) },
            ) {
                SearchScreen(
                    onStockClick = { navController.navigate(detailRoute(it)) },
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                )
            }

            composable(
                route = DETAIL_ROUTE,
                arguments = listOf(navArgument("symbol") { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                )
            }
        }
    }
}

@Composable
private fun MidasBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (AppDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        AppDestination.entries.forEach { destination ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == destination.route
            } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon
                        else destination.unselectedIcon,
                        contentDescription = destination.contentDescription,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
