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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilemeetsmobile.android.ui.components.displayClock
import com.mobilemeetsmobile.android.ui.components.displayShortDate
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantBrown
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel

@Composable
fun SpeakerProfileScreen(
    speakerId: String,
    speakersViewModel: SpeakersViewModel,
    scheduleViewModel: ScheduleViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSessionClick: (String) -> Unit,
) {
    val speakersState by speakersViewModel.uiState.collectAsState()
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val speaker = speakersState.speakers.firstOrNull { it.id == speakerId }
    val featuredSession = scheduleState.sessions.firstOrNull { speakerId in it.speakerIds }

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
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VibrantBrown)
            }
            Text(
                text = "MOBILE MEETS MOBILE",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp,
                color = VibrantBrown,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = VibrantText)
            }
        }
        Divider(color = VibrantBorder)

        if (speaker != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                EventLogo()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = speaker.name,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        color = VibrantText,
                    )
                    Text(
                        text = "${speaker.role}, ${speaker.company}",
                        style = MaterialTheme.typography.labelMedium,
                        color = IngOrange,
                        fontWeight = FontWeight.Bold,
                    )
                }

                SocialGlyphs()

                Text(
                    text = speaker.bio,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = VibrantText,
                )

                Divider(color = VibrantBorder, modifier = Modifier.padding(top = 10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "FEATURED SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.8.sp,
                        color = VibrantMuted,
                    )
                    if (featuredSession != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = VibrantBrown, modifier = Modifier.size(15.dp))
                            Text(
                                text = "${displayShortDate(featuredSession.startTime)}, ${displayClock(featuredSession.startTime)} • ${featuredSession.room}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VibrantBrown,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(featuredSession.title, style = MaterialTheme.typography.bodyMedium, color = VibrantText)
                        Text(
                            text = featuredSession.description,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = VibrantText,
                        )
                        Text(
                            text = "View session details",
                            modifier = Modifier.clickable { onSessionClick(featuredSession.id) },
                            style = MaterialTheme.typography.labelMedium,
                            color = VibrantBrown,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun EventLogo() {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 52.dp)
            .border(1.dp, VibrantBrown, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .background(VibrantSurface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.Bottom) {
                Text("m", color = IngOrange, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("m", color = Color(0xFFFFB000), fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("26", color = VibrantText, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Text("mobile meets mobile", color = VibrantBrown, fontSize = 5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SocialGlyphs() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("∞", "●", "<>").forEach {
            Text(it, style = MaterialTheme.typography.labelSmall, color = VibrantBrown, fontWeight = FontWeight.Bold)
        }
    }
}
