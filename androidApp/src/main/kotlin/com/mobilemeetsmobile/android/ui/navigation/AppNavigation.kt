package com.mobilemeetsmobile.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mobilemeetsmobile.android.ui.detail.SessionDetailScreen
import com.mobilemeetsmobile.android.ui.schedule.ScheduleScreen
import com.mobilemeetsmobile.android.ui.speakers.SpeakersScreen
import com.mobilemeetsmobile.android.ui.theme.*
import com.mobilemeetsmobile.presentation.detail.SessionDetailViewModel
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel
import org.koin.compose.koinInject

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Schedule : Screen("schedule", "Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Speakers : Screen("speakers", "Speakers", Icons.Filled.People, Icons.Outlined.People)
    data object SessionDetail : Screen("session/{sessionId}", "Detail", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
}

val bottomNavItems = listOf(Screen.Schedule, Screen.Speakers)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomBar) {
                MobileMeetsMobileBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                )
            }
        },
        topBar = {
            if (showBottomBar) {
                MobileMeetsMobileTopBar()
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Schedule.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Schedule.route) {
                val viewModel: ScheduleViewModel = koinInject()
                ScheduleScreen(
                    viewModel = viewModel,
                    onSessionClick = { sessionId ->
                        navController.navigate("session/$sessionId")
                    },
                )
            }

            composable(Screen.Speakers.route) {
                val viewModel: SpeakersViewModel = koinInject()
                SpeakersScreen(viewModel = viewModel)
            }

            composable(
                route = Screen.SessionDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val viewModel: SessionDetailViewModel = koinInject()
                SessionDetailScreen(
                    viewModel = viewModel,
                    sessionId = sessionId,
                    onBackClick = { navController.popBackStack() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileMeetsMobileTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("G", color = GoogleBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("o", color = GoogleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("o", color = GoogleYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("g", color = GoogleBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("l", color = GoogleGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("e", color = GoogleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("I/O", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = GoogleBlue.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "2025",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GoogleBlue,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DarkBackground,
        ),
    )
}

@Composable
fun MobileMeetsMobileBottomBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar(
        containerColor = DarkSurface,
        contentColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Schedule.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.label,
                    )
                },
                label = {
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoogleBlue,
                    selectedTextColor = GoogleBlue,
                    indicatorColor = GoogleBlue.copy(alpha = 0.15f),
                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f),
                ),
            )
        }
    }
}
