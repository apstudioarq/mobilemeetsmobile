package com.ing.event.ui.speakers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ing.event.ui.components.SpeakerAvatar
import com.ing.event.ui.theme.*
import com.ingevent.data.model.Speaker
import com.ingevent.presentation.speakers.SpeakersViewModel

@Composable
fun SpeakersScreen(
    viewModel: SpeakersViewModel,
    onSpeakerClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Speakers",
            style = MaterialTheme.typography.headlineMedium,
            color = VibrantText,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        if (state.error != null && state.speakers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.error ?: "Unable to load speakers.",
                    color = VibrantMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::loadSpeakers) {
                    Text("Retry")
                }
            }
        } else if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IngOrange)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(
                    items = state.speakers,
                    key = { it.id },
                ) { speaker ->
                    SpeakerCard(
                        speaker = speaker,
                        onClick = { onSpeakerClick(speaker.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun SpeakerCard(
    speaker: Speaker,
    onClick: () -> Unit,
) {
    val colors = listOf(IngOrange, GoogleRed, IngSky, IngSun, TrackDesign, TrackFirebase)
    val color = colors[speaker.name.hashCode().mod(colors.size).let { if (it < 0) -it else it }]
    val initials = speaker.name.split(" ").map { it.first() }.joinToString("")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpeakerAvatar(
                initials = initials,
                color = color,
                size = 64,
                imageModel = speaker.photoImageSource.takeIf(String::isNotBlank),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = speaker.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = VibrantText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = speaker.role,
                style = MaterialTheme.typography.bodySmall,
                color = VibrantMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
