package com.mobilemeetsmobile.android.ui.speakers

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
import com.mobilemeetsmobile.android.ui.components.SpeakerAvatar
import com.mobilemeetsmobile.android.ui.theme.*
import com.mobilemeetsmobile.data.model.Speaker
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel

@Composable
fun SpeakersScreen(
    viewModel: SpeakersViewModel,
    onSpeakerClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Speakers",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IngOrange)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpeakerAvatar(initials = initials, color = color, size = 64)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = speaker.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = speaker.role,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
