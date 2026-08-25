package com.ing.event.ui.map

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ing.event.ui.theme.IngOrange
import com.ing.event.ui.theme.VibrantBackground
import com.ing.event.ui.theme.VibrantMuted
import com.ingevent.presentation.schedule.ScheduleViewModel

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

            bitmap != null -> ZoomableMapViewport(imageKey = content.imageBase64) { modifier ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Event map",
                    modifier = modifier,
                    contentScale = ContentScale.Fit,
                )
            }

            content.imageUrl.isNotBlank() -> ZoomableMapViewport(imageKey = content.imageUrl) { modifier ->
                AsyncImage(
                    model = content.imageUrl,
                    contentDescription = "Event map",
                    modifier = modifier,
                    contentScale = ContentScale.Fit,
                )
            }

            else -> MapMessage(message = "The event map is not available yet.")
        }
    }
}

@Composable
private fun ZoomableMapViewport(
    imageKey: String,
    content: @Composable (Modifier) -> Unit,
) {
    var scale by remember(imageKey) { mutableFloatStateOf(MIN_MAP_SCALE) }
    var offset by remember(imageKey) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size ->
                viewportSize = size
                offset = offset.coerceWithin(size, scale)
            }
            .pointerInput(imageKey) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > MIN_MAP_SCALE) {
                            scale = MIN_MAP_SCALE
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_MAP_SCALE
                            offset = Offset.Zero
                        }
                    },
                )
            }
            .pointerInput(imageKey, viewportSize) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val previousScale = scale
                    val nextScale = (previousScale * zoom).coerceIn(MIN_MAP_SCALE, MAX_MAP_SCALE)
                    val scaleRatio = nextScale / previousScale
                    val viewportCenter = Offset(
                        x = viewportSize.width / 2f,
                        y = viewportSize.height / 2f,
                    )
                    val centroidFromCenter = centroid - viewportCenter
                    val zoomedOffset = Offset(
                        x = offset.x * scaleRatio + centroidFromCenter.x * (1f - scaleRatio),
                        y = offset.y * scaleRatio + centroidFromCenter.y * (1f - scaleRatio),
                    )

                    scale = nextScale
                    offset = (zoomedOffset + pan).coerceWithin(viewportSize, nextScale)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    clip = true
                },
        )
    }
}

private fun Offset.coerceWithin(viewportSize: IntSize, scale: Float): Offset {
    if (scale <= MIN_MAP_SCALE || viewportSize == IntSize.Zero) return Offset.Zero

    val maxX = viewportSize.width * (scale - MIN_MAP_SCALE) / 2f
    val maxY = viewportSize.height * (scale - MIN_MAP_SCALE) / 2f
    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY),
    )
}

private const val MIN_MAP_SCALE = 1f
private const val DOUBLE_TAP_MAP_SCALE = 2.5f
private const val MAX_MAP_SCALE = 5f

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
