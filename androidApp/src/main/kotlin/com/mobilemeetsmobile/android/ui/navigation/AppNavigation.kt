package com.mobilemeetsmobile.android.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilemeetsmobile.android.R
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
import com.mobilemeetsmobile.android.ui.theme.VibrantSurfaceWarm
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

private fun NavHostController.navigateToBottomTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

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
                    sectionTitle = when (currentRoute) {
                        Screen.Home.route -> Screen.Home.label
                        Screen.Schedule.route -> Screen.Schedule.label
                        Screen.Favorites.route -> Screen.Favorites.label
                        else -> ""
                    },
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
                    onScheduleClick = { navController.navigateToBottomTab(Screen.Schedule) },
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

@Composable
fun MobileMeetsMobileTopBar(sectionTitle: String) {
    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibrantSurface),
    ) {
        Spacer(Modifier.height(statusBarHeight))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_logo),
                    contentDescription = "Mobile Meets Mobile",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Mobile Meets Mobile",
                    color = VibrantText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(VibrantSurfaceWarm)
                    .border(1.dp, VibrantBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Text(
                    text = sectionTitle.uppercase(),
                    color = IngOrange,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(VibrantBorder.copy(alpha = 0.65f)),
        )
    }
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
                        navController.navigateToBottomTab(screen)
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
