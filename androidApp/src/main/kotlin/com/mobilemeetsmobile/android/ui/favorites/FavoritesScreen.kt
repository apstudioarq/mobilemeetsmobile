package com.mobilemeetsmobile.android.ui.favorites

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilemeetsmobile.android.ui.components.SessionCard
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantBrown
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurfaceWarm
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel

@Composable
fun FavoritesScreen(
    viewModel: ScheduleViewModel,
    onSessionClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val savedSessions = state.allSessions
        .filter { session ->
            session.isBookmarked ||
                session.tags.any { it.equals("Saved", ignoreCase = true) }
        }
        .distinctBy { it.id }
    val categoryTracks = savedSessions
        .map { it.track }
        .distinct()
        .sortedBy { it.displayName }
    val filteredSavedSessions = savedSessions.filter { session ->
        state.selectedTrack == null || session.track == state.selectedTrack
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                text = "Saved\nSessions",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 46.sp, lineHeight = 52.sp),
                color = VibrantText,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Your personalized schedule. These are the talks and workshops you've marked as high priority.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 30.sp),
                color = VibrantMuted,
            )
            Spacer(Modifier.height(28.dp))
            CategoryFilter(
                tracks = categoryTracks,
                selectedTrack = state.selectedTrack,
                onTrackSelected = viewModel::selectTrack,
            )
            Spacer(Modifier.height(36.dp))
        }

        if (filteredSavedSessions.isEmpty()) {
            item {
                Text(
                    text = "No saved sessions match this category.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VibrantMuted,
                )
            }
        }

        items(filteredSavedSessions, key = { "saved_${it.id}" }) { session ->
            SessionCard(
                session = session,
                onClick = { onSessionClick(session.id) },
                onBookmarkClick = { viewModel.onBookmarkToggle(session.id) },
            )
        }

        item {
            Divider(color = VibrantBorder, modifier = Modifier.padding(top = 34.dp))
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recommended for\nyou",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 38.sp),
                        color = VibrantText,
                    )
                    Text(
                        text = "Curated based on your saved sessions.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VibrantMuted,
                    )
                }
                Text(
                    text = "View\nall",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = VibrantBrown,
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                items(state.allSessions.filter { it.tags.any { tag -> tag.equals("Recommended", ignoreCase = true) } }) {
                    RecommendedCard(session = it)
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
            .background(if (isSelected) IngOrange else VibrantBackground)
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
            color = if (isSelected) VibrantBackground else VibrantText,
        )
    }
}

@Composable
private fun RecommendedCard(session: Session) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .background(VibrantSurfaceWarm, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = session.type.displayName,
            modifier = Modifier
                .background(VibrantBackground, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = VibrantMuted,
        )
        Text(
            text = session.title,
            style = MaterialTheme.typography.titleLarge,
            color = VibrantText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = session.description,
            style = MaterialTheme.typography.bodyMedium,
            color = VibrantMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Tomorrow  •  10:00 AM",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = IngOrange,
        )
    }
}
