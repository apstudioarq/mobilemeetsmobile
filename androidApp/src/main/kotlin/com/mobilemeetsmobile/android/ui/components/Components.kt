package com.mobilemeetsmobile.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.mobilemeetsmobile.android.ui.theme.*
import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.SessionType
import com.mobilemeetsmobile.data.model.Track

@Composable
fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = session.track.accentColor()
    val trackBg = session.track.bgColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant,
        ),
    ) {
        // Left accent border
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(trackColor)
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Top row: badges + bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TypeBadge(type = session.type, trackColor = trackColor)
                        TrackBadge(track = session.track)
                        LevelBadge(level = session.level.displayName)
                    }
                    IconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        val bookmarkColor by animateColorAsState(
                            if (session.isBookmarked) GoogleYellow else Color.White.copy(alpha = 0.25f),
                            label = "bookmark",
                        )
                        Icon(
                            imageVector = if (session.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Bookmark",
                            tint = bookmarkColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Title
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Description
                Text(
                    text = session.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Bottom: meta info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetaText(icon = "🕐", text = session.duration)
                        MetaText(icon = "📍", text = session.room)
                    }
                }

                // Capacity bar
                CapacityBar(
                    registered = session.registered,
                    capacity = session.capacity,
                    trackColor = trackColor,
                )
            }
        }
    }
}

@Composable
fun TypeBadge(type: SessionType, trackColor: Color) {
    val isKeynote = type == SessionType.KEYNOTE
    val icon = when (type) {
        SessionType.KEYNOTE -> "★"
        SessionType.SESSION -> "▶"
        SessionType.WORKSHOP -> "⚙"
        SessionType.CODELAB -> "💻"
        SessionType.OFFICE_HOURS -> "🗣"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isKeynote) {
                    Brush.linearGradient(listOf(GoogleBlue, TrackDesign))
                } else {
                    Brush.linearGradient(listOf(trackColor.copy(alpha = 0.2f), trackColor.copy(alpha = 0.2f)))
                }
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$icon ${type.displayName}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isKeynote) Color.White else trackColor,
        )
    }
}

@Composable
fun TrackBadge(track: Track) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(track.bgColor())
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = track.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = track.accentColor(),
        )
    }
}

@Composable
fun LevelBadge(level: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = level,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun TrackFilterChip(
    track: String,
    isSelected: Boolean,
    color: Color = GoogleBlue,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) color else Color.White.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(
            text = track,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun DayTab(
    label: String,
    date: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(listOf(GoogleBlue, GoogleGreen))
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun CapacityBar(
    registered: Int,
    capacity: Int,
    trackColor: Color,
) {
    val fraction = (registered.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)
    val isFull = registered >= capacity

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isFull) {
                            Brush.horizontalGradient(listOf(GoogleRed, Color(0xFFFF6B6B)))
                        } else {
                            Brush.horizontalGradient(listOf(trackColor, trackColor.copy(alpha = 0.5f)))
                        }
                    ),
            )
        }
        Text(
            text = if (isFull) "FULL" else "${capacity - registered} spots left",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.35f),
        )
    }
}

@Composable
fun MetaText(icon: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 12.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.35f),
        )
    }
}

@Composable
fun TimeSlotHeader(time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(GoogleBlue, GoogleGreen))
                ),
        )
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.4f),
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.06f),
        )
    }
}

@Composable
fun SpeakerAvatar(
    initials: String,
    color: Color,
    size: Int = 40,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(color, color.copy(alpha = 0.6f)))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = (size / 3).sp,
        )
    }
}
