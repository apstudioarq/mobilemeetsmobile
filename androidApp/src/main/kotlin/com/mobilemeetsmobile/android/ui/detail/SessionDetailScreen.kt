package com.mobilemeetsmobile.android.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.mobilemeetsmobile.presentation.detail.SessionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    sessionId: String,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val session = state.session

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        if (state.isLoading || session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoogleBlue)
            }
        } else {
            val trackColor = session.track.accentColor()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Hero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(trackColor.copy(alpha = 0.18f), DarkBackground)
                            )
                        )
                        .padding(24.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TypeBadge(type = session.type, trackColor = trackColor)
                            TrackBadge(track = session.track)
                            LevelBadge(level = session.level.displayName)
                        }

                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                        )

                        Text(
                            text = session.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f),
                        )

                        // Meta row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetaInfoRow(icon = "📅", label = "Date", value = "Day ${session.day} · ${session.startTime}")
                            MetaInfoRow(icon = "⏱", label = "Duration", value = session.duration)
                            MetaInfoRow(icon = "📍", label = "Location", value = session.room)
                            MetaInfoRow(icon = "📊", label = "Level", value = session.level.displayName)
                        }

                        // Action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.onBookmarkToggle() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (session.isBookmarked) GoogleYellow else Color.White.copy(alpha = 0.1f),
                                    contentColor = if (session.isBookmarked) Color.Black else Color.White,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    if (session.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (session.isBookmarked) "Saved" else "Save to Schedule")
                            }

                            Button(
                                onClick = { /* Reserve seat */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = trackColor,
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Reserve Seat")
                            }
                        }
                    }
                }

                // Speakers section
                if (state.speakers.isNotEmpty()) {
                    Text(
                        text = "Speakers",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )

                    state.speakers.forEach { speaker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SpeakerAvatar(
                                initials = speaker.name.split(" ").map { it.first() }.joinToString(""),
                                color = trackColor,
                                size = 52,
                            )
                            Column {
                                Text(
                                    text = speaker.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                                Text(
                                    text = speaker.role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }

                // Capacity
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Availability",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    CapacityBar(
                        registered = session.registered,
                        capacity = session.capacity,
                        trackColor = trackColor,
                    )
                    Text(
                        text = "${session.registered} / ${session.capacity} registered",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MetaInfoRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleMedium)
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
    }
}
