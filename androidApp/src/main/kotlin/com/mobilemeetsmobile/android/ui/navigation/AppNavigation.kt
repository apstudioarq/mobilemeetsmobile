package com.mobilemeetsmobile.android.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
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
    val scheduleViewModel: ScheduleViewModel = koinInject()
    val speakersViewModel: SpeakersViewModel = koinInject()
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    var isScheduleSearchExpanded by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBars = currentRoute in bottomNavItems.map { it.route }
    val isScheduleRoute = currentRoute == Screen.Schedule.route

    Scaffold(
        containerColor = VibrantBackground,
        topBar = {
            if (showBars) {
                Column {
                    MobileMeetsMobileTopBar(
                        sectionTitle = when (currentRoute) {
                            Screen.Home.route -> Screen.Home.label
                            Screen.Schedule.route -> Screen.Schedule.label
                            Screen.Favorites.route -> Screen.Favorites.label
                            else -> ""
                        },
                        searchQuery = scheduleState.searchQuery,
                        onSearchQueryChanged = scheduleViewModel::onSearchQueryChanged,
                        showSearchAction = isScheduleRoute,
                        isSearchExpanded = isScheduleRoute && isScheduleSearchExpanded,
                        onSearchActionClick = { isScheduleSearchExpanded = !isScheduleSearchExpanded },
                    )
                    if (scheduleState.isOffline && !scheduleState.requiresConnection) {
                        OfflineModeBanner()
                    }
                }
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
                HomeScreen(
                    scheduleViewModel = scheduleViewModel,
                    speakersViewModel = speakersViewModel,
                    onSessionClick = { navController.navigate("session/$it") },
                    onSpeakerClick = { navController.navigate("speaker/$it") },
                    onScheduleClick = { navController.navigateToBottomTab(Screen.Schedule) },
                )
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    viewModel = scheduleViewModel,
                    onSessionClick = { navController.navigate("session/$it") },
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = scheduleViewModel,
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
                    onCloseClick = { navController.popBackStack() },
                    onSpeakerProfileClick = { navController.navigate("speaker/$it") },
                )
            }
            composable(
                route = Screen.SpeakerProfile.route,
                arguments = listOf(navArgument("speakerId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val speakerId = backStackEntry.arguments?.getString("speakerId") ?: return@composable
                SpeakerProfileScreen(
                    speakerId = speakerId,
                    speakersViewModel = speakersViewModel,
                    scheduleViewModel = scheduleViewModel,
                    onCloseClick = { navController.popBackStack() },
                    onSessionClick = { sessionId -> navController.navigate("session/$sessionId") },
                )
            }
        }
    }

    if (scheduleState.requiresConnection) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Internet connection required")
            },
            text = {
                Text(
                    "No saved event data is available on this device. " +
                        "Connect to the internet and try again.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scheduleViewModel.retryConnection()
                        speakersViewModel.loadSpeakers()
                    },
                ) {
                    Text("Try again", color = IngOrange)
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        )
    }
}

@Composable
private fun OfflineModeBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(IngOrange.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Offline mode · Showing saved data",
            color = IngOrange,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun MobileMeetsMobileTopBar(
    sectionTitle: String,
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    showSearchAction: Boolean = false,
    isSearchExpanded: Boolean = false,
    onSearchActionClick: () -> Unit = {},
) {
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
                .height(58.dp)
                .padding(horizontal = 16.dp),
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
                        .clip(RoundedCornerShape(7.dp)),
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    text = sectionTitle,
                    color = VibrantText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showSearchAction) {
                IconButton(
                    onClick = onSearchActionClick,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Toggle search",
                        tint = IngOrange,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        if (isSearchExpanded) {
            TopBarSearchField(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
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
private fun TopBarSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        textStyle = TextStyle(
            color = VibrantText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(VibrantSurfaceWarm)
                    .border(1.dp, VibrantBorder, RoundedCornerShape(5.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search sessions or speakers",
                    tint = IngOrange,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isBlank()) {
                        Text(
                            text = "Search sessions or speakers",
                            color = VibrantMuted,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotBlank()) {
                    Spacer(Modifier.width(7.dp))
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = VibrantMuted,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable { onQueryChanged("") },
                    )
                }
            }
        },
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
                        text = screen.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IngOrange,
                    selectedTextColor = IngOrange,
                    indicatorColor = Color.Transparent,
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
