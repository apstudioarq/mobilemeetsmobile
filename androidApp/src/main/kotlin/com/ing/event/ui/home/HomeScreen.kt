package com.ing.event.ui.home

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ing.event.R
import com.ing.event.ui.components.LiveSessionCard
import com.ing.event.ui.components.SpeakerRowCard
import com.ing.event.ui.components.displayClock
import com.ing.event.ui.theme.IngOrange
import com.ing.event.ui.theme.VibrantBackground
import com.ing.event.ui.theme.VibrantBorder
import com.ing.event.ui.theme.VibrantBrown
import com.ing.event.ui.theme.VibrantMuted
import com.ing.event.ui.theme.VibrantSurface
import com.ing.event.ui.theme.VibrantSurfaceWarm
import com.ing.event.ui.theme.VibrantText
import com.ingevent.data.model.HomeContent
import com.ingevent.presentation.schedule.ScheduleViewModel
import com.ingevent.presentation.speakers.SpeakersViewModel

@Composable
fun HomeScreen(
    scheduleViewModel: ScheduleViewModel,
    speakersViewModel: SpeakersViewModel,
    onSessionClick: (String) -> Unit,
    onSpeakerClick: (String) -> Unit,
    onScheduleClick: () -> Unit,
) {
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val speakersState by speakersViewModel.uiState.collectAsState()
    val sessions = scheduleState.homeSessions
    val speakers = speakersState.speakers
    val speakersById = speakers.associateBy { it.id }
    val featuredSpeakers = if (scheduleState.homeSessionsAreLive) {
        sessions
            .flatMap { it.speakerIds }
            .distinct()
            .mapNotNull(speakersById::get)
    } else {
        emptyList()
    }
    val sessionSectionTitle = when {
        scheduleState.homeSessionsAreLive -> "Live now"
        sessions.isNotEmpty() -> "Up next"
        else -> "Coming soon"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(content = scheduleState.homeContent)
        }

        item {
            SectionHeader(
                title = sessionSectionTitle,
                action = "View schedule",
                onAction = onScheduleClick,
            )
        }

        items(sessions, key = { "home_session_${it.id}" }) { session ->
            val names = speakers
                .filter { it.id in session.speakerIds }
                .joinToString { it.name }
                .ifBlank { "ING Event" }
            LiveSessionCard(
                session = session,
                speakerNames = names,
                statusLabel = if (scheduleState.homeSessionsAreLive) {
                    "LIVE"
                } else {
                    "UP NEXT · ${displayClock(session.startTime)}"
                },
                onClick = { onSessionClick(session.id) },
            )
        }

        if (sessions.isEmpty()) {
            item {
                Text(
                    text = "There are no live sessions right now. Check back soon.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VibrantMuted,
                )
            }
        }

        if (featuredSpeakers.isNotEmpty()) {
            item {
                SectionHeader(title = "Featured speakers")
            }

            items(featuredSpeakers, key = { "speaker_${it.id}" }) { speaker ->
                SpeakerRowCard(
                    speaker = speaker,
                    onClick = { onSpeakerClick(speaker.id) },
                )
            }
        }

        item {
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun HeroCard(content: HomeContent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VibrantSurface)
            .border(1.dp, VibrantBorder.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IngOrange)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ING EVENT",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.84f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HeroImage(imageBase64 = content.imageBase64, imageUrl = content.imageUrl)
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(VibrantBrown),
            )
            Text(
                text = content.description,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = VibrantMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroImage(
    imageBase64: String,
    imageUrl: String,
) {
    val base64Bitmap = remember(imageBase64) {
        imageBase64.decodeBase64Bitmap()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .background(VibrantSurfaceWarm),
        contentAlignment = Alignment.Center,
    ) {
        if (base64Bitmap != null) {
            Image(
                bitmap = base64Bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "ING Event",
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
    }
}

private fun String.decodeBase64Bitmap() = runCatching {
    val cleanBase64 = trim()
        .substringAfter("base64,", missingDelimiterValue = this)
        .trim()
    if (cleanBase64.isBlank() || cleanBase64.startsWith("http", ignoreCase = true)) return@runCatching null

    val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = VibrantText,
            )
            if (action != null) {
                Text(
                    text = action,
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .clickable(onClick = onAction),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = VibrantBrown,
                )
            }
        }
        Divider(color = VibrantBorder, thickness = 1.dp)
    }
}
