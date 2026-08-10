package com.mobilemeetsmobile.android.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel

@Composable
fun FavoritesScreen(
    viewModel: ScheduleViewModel,
    onSessionClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val savedSessions = state.sessions
        .filter { session ->
            session.isBookmarked ||
                session.tags.any { it.equals("Saved", ignoreCase = true) } ||
                session.id in setOf("async-workflows", "cognitive-load", "design-systems-scale")
        }
        .distinctBy { it.id }

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
            Spacer(Modifier.height(48.dp))
        }

        items(savedSessions, key = { "saved_${it.id}" }) { session ->
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
                items(state.sessions.filter { it.tags.any { tag -> tag.equals("Recommended", ignoreCase = true) } }) {
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
