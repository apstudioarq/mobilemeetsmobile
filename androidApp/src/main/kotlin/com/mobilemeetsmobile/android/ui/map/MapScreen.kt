package com.mobilemeetsmobile.android.ui.map

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel

@Composable
fun MapScreen(viewModel: ScheduleViewModel) {
    val state by viewModel.uiState.collectAsState()
    val content = state.mapContent
    val bitmap = remember(content.imageBase64) { content.imageBase64.decodeBase64Bitmap() }

    LaunchedEffect(Unit) {
        viewModel.loadMapContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isMapLoading -> CircularProgressIndicator(color = IngOrange)

            state.mapError != null && !content.hasImage -> MapMessage(
                message = state.mapError ?: "Unable to load the map.",
                actionLabel = "Try again",
                onAction = viewModel::loadMapContent,
            )

            bitmap != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Event map",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }

            content.imageUrl.isNotBlank() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                AsyncImage(
                    model = content.imageUrl,
                    contentDescription = "Event map",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }

            else -> MapMessage(message = "The event map is not available yet.")
        }
    }
}

@Composable
private fun MapMessage(
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            color = VibrantMuted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = IngOrange),
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun String.decodeBase64Bitmap() = runCatching {
    val cleanBase64 = trim()
        .substringAfter("base64,", missingDelimiterValue = this)
        .trim()
    if (cleanBase64.isBlank()) return@runCatching null

    val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()
