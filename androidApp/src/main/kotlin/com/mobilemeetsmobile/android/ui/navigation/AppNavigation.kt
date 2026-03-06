package com.mobilemeetsmobile.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.ui.Alignment

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
//                Text("M", color = GoogleBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
//                Text("o", color = GoogleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//                Text("b", color = GoogleYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//                Text("i", color = GoogleBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//                Text("l", color = GoogleGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//                Text("e", color = GoogleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//                Spacer(Modifier.width(6.dp))
                Text("Mobile meets Mobile", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = IngOrange.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "2026",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IngOrange,
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
                    selectedIconColor = IngOrange,
                    selectedTextColor = IngOrange,
                    indicatorColor = IngOrange.copy(alpha = 0.15f),
                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppNavigationWithSchedulePreview() {
    MaterialTheme {
        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                MobileMeetsMobileTopBar()
            },
            bottomBar = {
                MobileMeetsMobileBottomBar(
                    navController = rememberNavController(),
                    currentRoute = Screen.Schedule.route,
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DarkBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Tabs de días
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = IngOrange,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Day 1",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color.White
                            )
                        }
                        Surface(
                            color = DarkSurface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Day 2",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filtros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Filtro "All"
                        Surface(
                            color = IngOrange,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "All",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        // Filtro "Saved"
                        Surface(
                            color = DarkSurface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bookmark,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Saved",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }// Filtro por categoría
                        Surface(
                            color = DarkSurface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Android",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            color = DarkSurface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "iOS",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sesiones con bookmark
                    listOf(
                        Triple("Kotlin Multiplatform in Production", true, "Android"),
                        Triple("SwiftUI Best Practices", false, "iOS"),
                        Triple("Compose Navigation Deep Dive", true, "Android")
                    ).forEachIndexed { index, (title, isSaved, category) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = DarkSurface,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "10:0${index} - 11:0${index}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Speaker ${index + 1}",
                                            color = IngOrange,
                                            fontSize = 12.sp
                                        )
                                        Surface(
                                            color = if (category == "Android") IngSky.copy(alpha = 0.2f)
                                            else IngOrange.copy(alpha = 0.2f),
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = category,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = if (category == "Android") IngSky else IngOrange,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isSaved) IngSun else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

