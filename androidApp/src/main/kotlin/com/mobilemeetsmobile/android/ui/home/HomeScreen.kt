package com.mobilemeetsmobile.android.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mobilemeetsmobile.android.ui.components.LiveSessionCard
import com.mobilemeetsmobile.android.ui.components.SpeakerRowCard
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantBrown
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantSurfaceWarm
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.data.model.HomeContent
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel

@Composable
fun HomeScreen(
    scheduleViewModel: ScheduleViewModel,
    speakersViewModel: SpeakersViewModel,
    onSessionClick: (String) -> Unit,
    onScheduleClick: () -> Unit,
) {
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val speakersState by speakersViewModel.uiState.collectAsState()
    val sessions = scheduleState.sessions
    val speakers = speakersState.speakers

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        item {
            HeroCard(content = scheduleState.homeContent)
        }

        item {
            SectionHeader(
                title = "Live Now",
                action = "View Schedule",
                onAction = onScheduleClick,
            )
        }

        items(sessions.take(2), key = { "live_${it.id}" }) { session ->
            val names = speakers
                .filter { it.id in session.speakerIds }
                .joinToString { it.name }
                .ifBlank { "Mobile Meets Mobile" }
            LiveSessionCard(
                session = session,
                speakerNames = names,
                onClick = { onSessionClick(session.id) },
            )
        }

        item {
            SectionHeader(title = "Keynote Speakers")
        }

        items(speakers.take(3), key = { "speaker_${it.id}" }) { speaker ->
            SpeakerRowCard(speaker = speaker)
        }

        item {
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun HeroCard(content: HomeContent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(VibrantSurface)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(VibrantSurfaceWarm)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "Global Summit 2024",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = VibrantMuted,
                )
            }
            Text(
                text = "Mobile Meets Mobile",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = VibrantText,
            )
            Text(
                text = content.welcomeMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = VibrantMuted,
                maxLines = 4,
            )
        }

        HeroImage(imageUrl = content.heroImageUrl)
    }
}

@Composable
private fun HeroImage(imageUrl: String) {
    Box(
        modifier = Modifier
            .size(108.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF30302F)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(37.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFC8FFFF),
                                Color(0xFF61DDE4).copy(alpha = 0.55f),
                                Color.Transparent,
                            )
                        )
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF72E9F1).copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
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
        Divider(color = VibrantBorder)
    }
}
