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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
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
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = VibrantText)
            }
            Spacer(modifier = Modifier.weight(1f))
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Speaker", style = MaterialTheme.typography.headlineMedium, color = VibrantText)
                        state.speakers.forEach { speaker ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp))
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSpeakerProfileClick(speaker.id) }
                                    .background(VibrantSurface)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SpeakerAvatar(
                                    initials = initials(speaker.name),
                                    color = avatarColor(speaker.name),
                                    size = 52,
                                    imageModel = speaker.photoImageSource.takeIf(String::isNotBlank),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = speaker.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VibrantText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = listOf(speaker.role, speaker.company)
                                            .filter(String::isNotBlank)
                                            .joinToString(", "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VibrantMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (speaker.bio.isNotBlank()) {
                                        Text(
                                            text = speaker.bio,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VibrantMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "View speaker profile",
                                    tint = IngOrange,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }

                FeedbackCard(
                    rating = state.feedbackRating,
                    comment = state.feedbackComment,
                    isSubmitting = state.isSubmittingFeedback,
                    isSubmitted = state.feedbackSubmitted,
                    error = state.feedbackError,
                    onRatingSelected = viewModel::onRatingSelected,
                    onCommentChanged = viewModel::onFeedbackCommentChanged,
                    onSubmit = viewModel::submitFeedback,
                )
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
    val borderColor = if (isBookmarked) IngOrange.copy(alpha = 0.55f) else VibrantBorder
    val statusBackground = if (isBookmarked) IngOrange.copy(alpha = 0.12f) else VibrantSurfaceWarm
    val statusText = if (isBookmarked) "Saved" else "Not saved"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        VibrantSurface,
                        if (isBookmarked) IngOrange.copy(alpha = 0.06f) else VibrantSurface,
                    ),
                ),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IngOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = IngOrange,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Save this session", style = MaterialTheme.typography.headlineSmall, color = VibrantText)
                    Text(
                        text = if (isBookmarked) {
                            "This talk is in your Saved Sessions."
                        } else {
                            "Keep this talk handy and sync it with your schedule."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibrantMuted,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(statusBackground)
                    .border(1.dp, borderColor, RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isBookmarked) IngOrange else VibrantMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Button(
            onClick = onBookmark,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBookmarked) VibrantText else IngOrange,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isBookmarked) "Saved Session" else "Save Session", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedbackCard(
    rating: Int,
    comment: String,
    isSubmitting: Boolean,
    isSubmitted: Boolean,
    error: String?,
    onRatingSelected: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                repeat(5) { index ->
                    val starValue = index + 1
                    IconButton(
                        onClick = { onRatingSelected(starValue) },
                        enabled = !isSubmitting && !isSubmitted,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = if (starValue <= rating) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarBorder
                            },
                            contentDescription = "$starValue stars",
                            tint = if (starValue <= rating) IngOrange else VibrantBorder,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
            Text("Additional comments (optional)", style = MaterialTheme.typography.labelLarge, color = VibrantText)
            OutlinedTextField(
                value = comment,
                onValueChange = onCommentChanged,
                enabled = !isSubmitting && !isSubmitted,
                placeholder = { Text("What did you like or what could be improved?") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = VibrantSurfaceWarm,
                    unfocusedContainerColor = VibrantSurfaceWarm,
                    focusedIndicatorColor = VibrantBorder,
                    unfocusedIndicatorColor = VibrantBorder,
                ),
            )
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3261E),
                )
            } else if (isSubmitted) {
                Text(
                    text = "Thanks! Your feedback was submitted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onSubmit,
                enabled = rating in 1..5 && !isSubmitting && !isSubmitted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantBrown, contentColor = Color.White),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (isSubmitted) "Feedback Submitted" else "Submit Feedback",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
