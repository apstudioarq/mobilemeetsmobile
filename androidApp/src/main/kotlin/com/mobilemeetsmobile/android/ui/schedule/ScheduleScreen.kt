package com.mobilemeetsmobile.android.ui.schedule

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilemeetsmobile.android.ui.components.*
import com.mobilemeetsmobile.android.ui.theme.*
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onSessionClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    val allTracks = listOf("All") + Track.entries.map { it.displayName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Search sessions, speakers...",
                    color = Color.White.copy(alpha = 0.3f),
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                )
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoogleBlue.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
                focusedContainerColor = Color.White.copy(alpha = 0.06f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                cursorColor = GoogleBlue,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            singleLine = true,
        )

        // Day tabs
        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.days) { day ->
                DayTab(
                    label = day.label,
                    date = day.date,
                    isSelected = state.selectedDay == day.dayNumber,
                    onClick = { viewModel.loadDay(day.dayNumber) },
                )
            }
        }

        // Track filter
        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(allTracks) { trackName ->
                val track = if (trackName == "All") null else Track.entries.find { it.displayName == trackName }
                val color = track?.accentColor() ?: GoogleBlue
                TrackFilterChip(
                    track = trackName,
                    isSelected = (track == null && state.selectedTrack == null) ||
                        (track != null && state.selectedTrack == track),
                    color = color,
                    onClick = { viewModel.selectTrack(track) },
                )
            }
        }

        // Bookmarks toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            FilterChip(
                selected = state.showBookmarksOnly,
                onClick = { viewModel.toggleShowBookmarksOnly() },
                label = {
                    Text(
                        "Saved (${state.bookmarkedIds.size})",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        if (state.showBookmarksOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoogleYellow.copy(alpha = 0.2f),
                    selectedLabelColor = GoogleYellow,
                    selectedLeadingIconColor = GoogleYellow,
                ),
            )
        }

        // Content
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GoogleBlue)
            }
        } else if (state.timeSlots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No sessions match your filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val sortedSlots = state.timeSlots.entries.sortedBy { it.key }

                sortedSlots.forEach { (time, sessions) ->
                    item(key = "header_$time") {
                        TimeSlotHeader(time = time)
                    }

                    items(
                        items = sessions,
                        key = { it.id },
                    ) { session ->
                        SessionCard(
                            session = session,
                            onClick = { onSessionClick(session.id) },
                            onBookmarkClick = { viewModel.onBookmarkToggle(session.id) },
                            modifier = Modifier
                                .padding(start = 22.dp)
                                .animateItem(),
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
