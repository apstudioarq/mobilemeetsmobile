package com.mobilemeetsmobile.android.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobilemeetsmobile.android.ui.detail.SessionDetailScreen
import com.mobilemeetsmobile.android.ui.favorites.FavoritesScreen
import com.mobilemeetsmobile.android.ui.home.HomeScreen
import com.mobilemeetsmobile.android.ui.schedule.ScheduleScreen
import com.mobilemeetsmobile.android.ui.speakerprofile.SpeakerProfileScreen
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantText
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
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Schedule : Screen("schedule", "Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Favorites : Screen("favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    data object SessionDetail : Screen("session/{sessionId}", "Detail", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object SpeakerProfile : Screen("speaker/{speakerId}", "Speaker", Icons.Filled.Home, Icons.Outlined.Home)
}

val bottomNavItems = listOf(Screen.Home, Screen.Schedule, Screen.Favorites)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBars = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = VibrantBackground,
        topBar = {
            if (showBars) {
                MobileMeetsMobileTopBar(
                    title = if (currentRoute == Screen.Home.route) "Meets" else "Mobile Meets Mobile",
                )
            }
        },
        bottomBar = {
            if (showBars) {
                MobileMeetsMobileBottomBar(navController, currentRoute)
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Home.route) {
                val scheduleViewModel: ScheduleViewModel = koinInject()
                val speakersViewModel: SpeakersViewModel = koinInject()
                HomeScreen(
                    scheduleViewModel = scheduleViewModel,
                    speakersViewModel = speakersViewModel,
                    onSessionClick = { navController.navigate("session/$it") },
                    onScheduleClick = { navController.navigate(Screen.Schedule.route) },
                )
            }
            composable(Screen.Schedule.route) {
                val viewModel: ScheduleViewModel = koinInject()
                ScheduleScreen(
                    viewModel = viewModel,
                    onSessionClick = { navController.navigate("session/$it") },
                )
            }
            composable(Screen.Favorites.route) {
                val viewModel: ScheduleViewModel = koinInject()
                FavoritesScreen(
                    viewModel = viewModel,
                    onSessionClick = { navController.navigate("session/$it") },
                )
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
                    onSpeakerProfileClick = { navController.navigate("speaker/$it") },
                )
            }
            composable(
                route = Screen.SpeakerProfile.route,
                arguments = listOf(navArgument("speakerId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val speakerId = backStackEntry.arguments?.getString("speakerId") ?: return@composable
                val speakersViewModel: SpeakersViewModel = koinInject()
                val scheduleViewModel: ScheduleViewModel = koinInject()
                SpeakerProfileScreen(
                    speakerId = speakerId,
                    speakersViewModel = speakersViewModel,
                    scheduleViewModel = scheduleViewModel,
                    onBackClick = { navController.popBackStack() },
                    onCloseClick = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                    onSessionClick = { sessionId -> navController.navigate("session/$sessionId") },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileMeetsMobileTopBar(title: String) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                color = IngOrange,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = VibrantSurface,
            scrolledContainerColor = VibrantSurface,
        ),
    )
}

@Composable
fun MobileMeetsMobileBottomBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar(
        containerColor = VibrantSurface,
        contentColor = VibrantText,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
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
                        text = screen.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IngOrange,
                    selectedTextColor = IngOrange,
                    indicatorColor = Color(0xFFFFE9DF),
                    unselectedIconColor = VibrantMuted.copy(alpha = 0.7f),
                    unselectedTextColor = VibrantMuted.copy(alpha = 0.7f),
                ),
            )
        }
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 0.dp),
    )
}
