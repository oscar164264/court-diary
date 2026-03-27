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
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.courtdiary.ui.screens.*
import com.courtdiary.viewmodel.CaseViewModel
import com.courtdiary.viewmodel.PrecedentViewModel

object Routes {
    const val DASHBOARD      = "dashboard"
    const val ADD_CASE       = "add_case"
    const val ALL_CASES      = "all_cases"
    const val CALENDAR       = "calendar"
    const val PRECEDENTS     = "precedents"
    const val SETTINGS       = "settings"
    const val CASE_DETAIL    = "case_detail/{caseId}"
    const val EDIT_CASE      = "edit_case/{caseId}"
    const val ADD_PRECEDENT  = "add_precedent"
    const val PRECEDENT_DETAIL = "precedent_detail/{precedentId}"
    const val EDIT_PRECEDENT = "edit_precedent/{precedentId}"

    fun caseDetail(id: Int)      = "case_detail/$id"
    fun editCase(id: Int)        = "edit_case/$id"
    fun precedentDetail(id: Int) = "precedent_detail/$id"
    fun editPrecedent(id: Int)   = "edit_precedent/$id"
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

// 5 items — Settings is in the top-right corner of each screen
val bottomNavItems = listOf(
    BottomNavItem("Home",       Icons.Filled.Dashboard,    Routes.DASHBOARD),
    BottomNavItem("New Case",   Icons.Filled.AddCircle,    Routes.ADD_CASE),
    BottomNavItem("Cases",      Icons.Filled.Gavel,        Routes.ALL_CASES),
    BottomNavItem("Calendar",   Icons.Filled.CalendarMonth,Routes.CALENDAR),
    BottomNavItem("Precedents", Icons.Filled.LibraryBooks, Routes.PRECEDENTS),
)

private val hideBottomBarRoutes = setOf(
    Routes.CASE_DETAIL, Routes.EDIT_CASE,
    Routes.ADD_PRECEDENT, Routes.PRECEDENT_DETAIL, Routes.EDIT_PRECEDENT,
    Routes.SETTINGS
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtDiaryNavHost(
    viewModel: CaseViewModel,
    precedentViewModel: PrecedentViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route?.let { route ->
        hideBottomBarRoutes.none { route.startsWith(it.substringBefore("{")) }
    } ?: true

    val onSettings = { navController.navigate(Routes.SETTINGS) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 8.dp) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
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
            enterTransition  = { fadeIn(tween(180)) + slideInHorizontally({ it / 5 }, tween(180)) },
            exitTransition   = { fadeOut(tween(180)) + slideOutHorizontally({ -it / 5 }, tween(180)) },
            popEnterTransition  = { fadeIn(tween(180)) + slideInHorizontally({ -it / 5 }, tween(180)) },
            popExitTransition   = { fadeOut(tween(180)) + slideOutHorizontally({ it / 5 }, tween(180)) }
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onCaseClick = { navController.navigate(Routes.caseDetail(it)) },
                    onSettings = onSettings
                )
            }
            composable(Routes.ADD_CASE) {
                AddEditCaseScreen(viewModel = viewModel, caseId = null,
                    onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.ALL_CASES) {
                AllCasesScreen(
                    viewModel = viewModel,
                    onCaseClick = { navController.navigate(Routes.caseDetail(it)) },
                    onAddCase = { navController.navigate(Routes.ADD_CASE) },
                    onSettings = onSettings
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    viewModel = viewModel,
                    onCaseClick = { navController.navigate(Routes.caseDetail(it)) },
                    onSettings = onSettings
                )
            }
            composable(Routes.PRECEDENTS) {
                PrecedentsScreen(
                    viewModel = precedentViewModel,
                    onAddPrecedent = { navController.navigate(Routes.ADD_PRECEDENT) },
                    onPrecedentClick = { navController.navigate(Routes.precedentDetail(it)) },
                    onSettings = onSettings
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Case routes
            composable(Routes.CASE_DETAIL,
                arguments = listOf(navArgument("caseId") { type = NavType.IntType })
            ) {
                val id = it.arguments?.getInt("caseId") ?: return@composable
                CaseDetailScreen(viewModel = viewModel, caseId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onEditCase = { navController.navigate(Routes.editCase(id)) })
            }
            composable(Routes.EDIT_CASE,
                arguments = listOf(navArgument("caseId") { type = NavType.IntType })
            ) {
                val id = it.arguments?.getInt("caseId") ?: return@composable
                AddEditCaseScreen(viewModel = viewModel, caseId = id,
                    onNavigateBack = { navController.popBackStack() })
            }

            // Precedent routes
            composable(Routes.ADD_PRECEDENT) {
                AddEditPrecedentScreen(viewModel = precedentViewModel, precedentId = null,
                    onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.PRECEDENT_DETAIL,
                arguments = listOf(navArgument("precedentId") { type = NavType.IntType })
            ) {
                val id = it.arguments?.getInt("precedentId") ?: return@composable
                PrecedentDetailScreen(viewModel = precedentViewModel, precedentId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.editPrecedent(id)) })
            }
            composable(Routes.EDIT_PRECEDENT,
                arguments = listOf(navArgument("precedentId") { type = NavType.IntType })
            ) {
                val id = it.arguments?.getInt("precedentId") ?: return@composable
                AddEditPrecedentScreen(viewModel = precedentViewModel, precedentId = id,
                    onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
