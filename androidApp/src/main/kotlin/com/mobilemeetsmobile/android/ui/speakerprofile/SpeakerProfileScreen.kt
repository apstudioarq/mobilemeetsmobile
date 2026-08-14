package com.mobilemeetsmobile.android.ui.speakerprofile

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mobilemeetsmobile.android.ui.components.SpeakerAvatar
import com.mobilemeetsmobile.android.ui.components.avatarColor
import com.mobilemeetsmobile.android.ui.components.displayClock
import com.mobilemeetsmobile.android.ui.components.displayShortDate
import com.mobilemeetsmobile.android.ui.components.initials
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantBrown
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel

@Composable
fun SpeakerProfileScreen(
    speakerId: String,
    speakersViewModel: SpeakersViewModel,
    scheduleViewModel: ScheduleViewModel,
    onCloseClick: () -> Unit,
    onSessionClick: (String) -> Unit,
) {
    val speakersState by speakersViewModel.uiState.collectAsState()
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val speaker = speakersState.speakers.firstOrNull { it.id == speakerId }
    val speakerSessions = scheduleState.allSessions
        .filter { speakerId in it.speakerIds }
        .ifEmpty { scheduleState.sessions.filter { speakerId in it.speakerIds } }
        .sortedBy { it.startTime }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = IngOrange)
            }
        }
        Divider(color = VibrantBorder)

        if (speaker != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VibrantBorder, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(VibrantSurface)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpeakerAvatar(
                            initials = initials(speaker.name),
                            color = avatarColor(speaker.name),
                            size = 86,
                            imageModel = speaker.photoImageSource.takeIf(String::isNotBlank),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = speaker.name,
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 34.sp),
                                color = VibrantText,
                            )
                            val roleLine = listOf(speaker.role, speaker.company)
                                .filter(String::isNotBlank)
                                .joinToString(", ")
                            if (roleLine.isNotBlank()) {
                                Text(
                                    text = roleLine,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IngOrange,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (speaker.bio.isNotBlank()) {
                        Text(
                            text = speaker.bio,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = VibrantMuted,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SESSIONS",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.8.sp,
                        color = VibrantMuted,
                    )
                    if (speakerSessions.isEmpty()) {
                        Text(
                            text = "No sessions associated with this speaker.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibrantMuted,
                        )
                    } else {
                        speakerSessions.forEach { session ->
                            SpeakerSessionRow(
                                session = session,
                                onClick = { onSessionClick(session.id) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SpeakerSessionRow(
    session: Session,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VibrantBorder, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(VibrantSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyMedium,
                color = VibrantText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(
                    displayShortDate(session.startTime),
                    displayClock(session.startTime),
                    session.room,
                ).filter(String::isNotBlank).joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = VibrantBrown,
                fontWeight = FontWeight.Bold,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open session",
            tint = IngOrange,
            modifier = Modifier.size(22.dp),
        )
    }
}
