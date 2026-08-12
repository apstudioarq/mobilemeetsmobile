package com.mobilemeetsmobile.android.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mobilemeetsmobile.android.ui.components.DayTab
import com.mobilemeetsmobile.android.ui.components.SessionCard
import com.mobilemeetsmobile.android.ui.components.TimeSlotHeader
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onSessionClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.headlineMedium,
                color = VibrantText,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "The premier gathering for mobile innovators.",
                style = MaterialTheme.typography.bodyMedium,
                color = VibrantMuted,
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.days) { day ->
                    DayTab(
                        label = day.label,
                        date = day.date,
                        isSelected = state.selectedDay == day.dayNumber,
                        onClick = { viewModel.loadDay(day.dayNumber) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            val categoryTracks = state.allSessions
                .filter { it.day == state.selectedDay }
                .map { it.track }
                .distinct()
                .sortedBy { it.displayName }

            CategoryFilter(
                tracks = categoryTracks,
                selectedTrack = state.selectedTrack,
                onTrackSelected = viewModel::selectTrack,
            )
            Spacer(Modifier.height(28.dp))
        }

        if (state.error != null && state.sessions.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = state.error ?: "Unable to load the schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibrantMuted,
                    )
                    Button(onClick = { viewModel.loadDay(state.selectedDay) }) {
                        Text("Retry")
                    }
                }
            }
        } else if (state.isLoading && state.sessions.isEmpty()) {
            item {
                Row(Modifier.padding(40.dp)) {
                    CircularProgressIndicator(color = IngOrange)
                }
            }
        } else if (state.sessions.isEmpty()) {
            item {
                Text(
                    text = "No sessions match this category.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = VibrantMuted,
                )
            }
        } else {
            val sortedSlots = state.timeSlots.entries.sortedBy { it.key }
            sortedSlots.forEach { (time, sessions) ->
                item(key = "time_$time") {
                    TimeSlotHeader(time = time)
                }
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.id) },
                        onBookmarkClick = { viewModel.onBookmarkToggle(session.id) },
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun CategoryFilter(
    tracks: List<Track>,
    selectedTrack: Track?,
    onTrackSelected: (Track?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = VibrantMuted,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                CategoryChip(
                    label = "All",
                    isSelected = selectedTrack == null,
                    onClick = { onTrackSelected(null) },
                )
            }
            items(tracks, key = { it.name }) { track ->
                CategoryChip(
                    label = track.displayName,
                    isSelected = selectedTrack == track,
                    onClick = { onTrackSelected(track) },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) IngOrange else VibrantSurface)
            .border(
                1.dp,
                if (isSelected) IngOrange else VibrantBorder,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) VibrantSurface else VibrantText,
        )
    }
}
