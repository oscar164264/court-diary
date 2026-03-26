package com.courtdiary.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.courtdiary.ui.screens.*
import com.courtdiary.viewmodel.CaseViewModel

// ──────────────────────────────────────────
// Route constants
// ──────────────────────────────────────────
object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_CASE = "add_case"
    const val ALL_CASES = "all_cases"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val CASE_DETAIL = "case_detail/{caseId}"
    const val EDIT_CASE = "edit_case/{caseId}"

    fun caseDetail(id: Int) = "case_detail/$id"
    fun editCase(id: Int) = "edit_case/$id"
}

// ──────────────────────────────────────────
// Bottom nav items
// ──────────────────────────────────────────
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Filled.Dashboard, Routes.DASHBOARD),
    BottomNavItem("Add Case", Icons.Filled.AddCircle, Routes.ADD_CASE),
    BottomNavItem("Cases", Icons.Filled.Gavel, Routes.ALL_CASES),
    BottomNavItem("Calendar", Icons.Filled.CalendarMonth, Routes.CALENDAR),
    BottomNavItem("Settings", Icons.Filled.Settings, Routes.SETTINGS),
)

// Routes that should NOT show the bottom bar
private val hideBottomBarRoutes = setOf(Routes.CASE_DETAIL, Routes.EDIT_CASE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtDiaryNavHost(viewModel: CaseViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail/edit screens
    val showBottomBar = currentDestination?.route?.let { route ->
        hideBottomBarRoutes.none { route.startsWith(it.substringBefore("{")) }
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                    initialOffsetX = { it / 4 }, animationSpec = tween(200)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    targetOffsetX = { -it / 4 }, animationSpec = tween(200)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                    initialOffsetX = { -it / 4 }, animationSpec = tween(200)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    targetOffsetX = { it / 4 }, animationSpec = tween(200)
                )
            }
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onCaseClick = { caseId -> navController.navigate(Routes.caseDetail(caseId)) }
                )
            }

            composable(Routes.ADD_CASE) {
                AddEditCaseScreen(
                    viewModel = viewModel,
                    caseId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ALL_CASES) {
                AllCasesScreen(
                    viewModel = viewModel,
                    onCaseClick = { caseId -> navController.navigate(Routes.caseDetail(caseId)) },
                    onAddCase = { navController.navigate(Routes.ADD_CASE) }
                )
            }

            composable(Routes.CALENDAR) {
                CalendarScreen(
                    viewModel = viewModel,
                    onCaseClick = { caseId -> navController.navigate(Routes.caseDetail(caseId)) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }

            composable(
                route = Routes.CASE_DETAIL,
                arguments = listOf(navArgument("caseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val caseId = backStackEntry.arguments?.getInt("caseId") ?: return@composable
                CaseDetailScreen(
                    viewModel = viewModel,
                    caseId = caseId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditCase = { navController.navigate(Routes.editCase(caseId)) }
                )
            }

            composable(
                route = Routes.EDIT_CASE,
                arguments = listOf(navArgument("caseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val caseId = backStackEntry.arguments?.getInt("caseId") ?: return@composable
                AddEditCaseScreen(
                    viewModel = viewModel,
                    caseId = caseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
