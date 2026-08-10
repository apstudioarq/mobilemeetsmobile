package com.mobilemeetsmobile.android.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilemeetsmobile.android.ui.components.SpeakerAvatar
import com.mobilemeetsmobile.android.ui.components.TrackBadge
import com.mobilemeetsmobile.android.ui.components.TypeBadge
import com.mobilemeetsmobile.android.ui.components.avatarColor
import com.mobilemeetsmobile.android.ui.components.displayShortDate
import com.mobilemeetsmobile.android.ui.components.displayTimeRange
import com.mobilemeetsmobile.android.ui.components.initials
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantBrown
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantSurfaceWarm
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.presentation.detail.SessionDetailViewModel

@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    sessionId: String,
    onBackClick: () -> Unit,
    onSpeakerProfileClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val session = state.session

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VibrantMuted)
            }
            Text(
                text = "Back to Schedule",
                style = MaterialTheme.typography.labelLarge,
                color = VibrantMuted,
                fontWeight = FontWeight.Bold,
            )
        }
        Divider(color = VibrantBorder)

        if (state.isLoading || session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IngOrange)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                HeroImage()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeBadge(session.type)
                        TrackBadge(session.track)
                    }
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 42.sp),
                        color = VibrantText,
                    )
                    MetaLine(
                        icon = Icons.Filled.CalendarToday,
                        text = "${displayShortDate(session.startTime)}, 2024 • ${displayTimeRange(session)}",
                    )
                    MetaLine(icon = Icons.Filled.LocationOn, text = session.room)
                }

                Divider(color = VibrantBorder)

                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("About this session", style = MaterialTheme.typography.headlineMedium, color = VibrantText)
                    Text(
                        text = session.description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                        color = VibrantMuted,
                    )
                    Text(
                        text = "Attendees will learn practical frameworks for implementing rigid grid philosophies, balancing density with whitespace, and replacing heavy shadows with subtle tonal elevation strategies.",
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                        color = VibrantMuted,
                    )
                }

                ReserveCard(
                    isBookmarked = session.isBookmarked,
                    onBookmark = viewModel::onBookmarkToggle,
                )

                if (state.speakers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Speaker", style = MaterialTheme.typography.headlineMedium, color = VibrantText)
                        state.speakers.forEach { speaker ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VibrantSurface)
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SpeakerAvatar(initials(speaker.name), avatarColor(speaker.name), 58)
                                    Column {
                                        Text(speaker.name, style = MaterialTheme.typography.titleMedium, color = VibrantText)
                                        Text("${speaker.role}, ${speaker.company}", style = MaterialTheme.typography.labelMedium, color = VibrantMuted)
                                    }
                                }
                                Text(
                                    text = speaker.bio,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = VibrantMuted,
                                )
                                Text(
                                    text = "View full profile ->",
                                    modifier = Modifier.clickable { onSpeakerProfileClick(speaker.id) },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IngOrange,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                FeedbackCard()
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun HeroImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF061015)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, IngOrange, Color(0xFF18DDF2), Color.Transparent),
                    )
                ),
        )
        Box(
            modifier = Modifier
                .size(126.dp)
                .clip(RoundedCornerShape(63.dp))
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFA9FCFF), Color(0xFF1FB9E0).copy(alpha = 0.35f), Color.Transparent),
                    )
                ),
        )
    }
}

@Composable
private fun MetaLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = VibrantBrown, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = VibrantMuted)
    }
}

@Composable
private fun ReserveCard(isBookmarked: Boolean, onBookmark: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(VibrantSurface)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Reserve your seat", style = MaterialTheme.typography.headlineSmall, color = VibrantText)
        Text("Space is limited. Add to favorites to sync with your schedule.", style = MaterialTheme.typography.bodyMedium, color = VibrantMuted)
        Button(
            onClick = onBookmark,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IngOrange, contentColor = Color.White),
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(if (isBookmarked) " Saved" else " Add to Favorites", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VibrantText),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Share Session", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedbackCard() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Rate this session", style = MaterialTheme.typography.headlineMedium, color = VibrantText)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(VibrantSurface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("How was your experience with this session?", style = MaterialTheme.typography.bodyMedium, color = VibrantMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(4) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = IngOrange)
                }
                Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = VibrantBorder)
            }
            Text("Additional comments (optional)", style = MaterialTheme.typography.labelLarge, color = VibrantText)
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("What did you like or what could be improved?") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = VibrantSurfaceWarm,
                    unfocusedContainerColor = VibrantSurfaceWarm,
                    focusedIndicatorColor = VibrantBorder,
                    unfocusedIndicatorColor = VibrantBorder,
                ),
            )
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantBrown, contentColor = Color.White),
            ) {
                Text("Submit Feedback", fontWeight = FontWeight.Bold)
            }
        }
    }
}
