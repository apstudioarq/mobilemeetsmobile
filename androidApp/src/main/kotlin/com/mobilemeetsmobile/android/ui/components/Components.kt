package com.mobilemeetsmobile.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.mobilemeetsmobile.android.ui.theme.GoogleRed
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.TrackAiMl
import com.mobilemeetsmobile.android.ui.theme.TrackAndroid
import com.mobilemeetsmobile.android.ui.theme.TrackCloud
import com.mobilemeetsmobile.android.ui.theme.TrackDesign
import com.mobilemeetsmobile.android.ui.theme.TrackFirebase
import com.mobilemeetsmobile.android.ui.theme.TrackFlutter
import com.mobilemeetsmobile.android.ui.theme.TrackWeb
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantBrown
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSoftMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantSurfaceWarm
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.SessionType
import com.mobilemeetsmobile.data.model.Speaker
import com.mobilemeetsmobile.data.model.Track

@Composable
fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    TypeBadge(type = session.type)
                    TrackBadge(track = session.track)
                }
                StarToggle(
                    isSelected = session.isBookmarked || session.tags.any { it.equals("Saved", ignoreCase = true) },
                    onClick = onBookmarkClick,
                )
            }

            Text(
                text = session.title,
                style = MaterialTheme.typography.headlineSmall,
                color = VibrantText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = session.description,
                style = MaterialTheme.typography.bodyLarge,
                color = VibrantMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Divider(color = VibrantBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniSpeakerAvatar(session.speakerIds.firstOrNull().orEmpty())
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTimeRange(session),
                        style = MaterialTheme.typography.labelLarge,
                        color = VibrantBrown,
                    )
                    Text(
                        text = session.room.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantMuted,
                    )
                }
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IngOrange,
                    contentColor = Color.White,
                ),
            ) {
                Text("Reserve Seat", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LiveSessionCard(
    session: Session,
    speakerNames: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveBadge()
                Text(
                    text = session.room,
                    style = MaterialTheme.typography.labelSmall,
                    color = VibrantSoftMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = session.title,
                style = MaterialTheme.typography.headlineSmall,
                color = VibrantText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = session.description,
                style = MaterialTheme.typography.bodyLarge,
                color = VibrantMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniSpeakerAvatar(session.id)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = speakerNames,
                    style = MaterialTheme.typography.labelMedium,
                    color = VibrantMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SpeakerRowCard(speaker: Speaker, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VibrantBorder, RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeakerAvatar(initials = initials(speaker.name), color = avatarColor(speaker.name), size = 58)
            Column {
                Text(
                    text = speaker.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VibrantText,
                )
                Text(
                    text = "${speaker.role}, ${speaker.company}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibrantBrown,
                )
            }
        }
    }
}

@Composable
fun TypeBadge(type: SessionType) {
    val color = if (type == SessionType.KEYNOTE) IngOrange else VibrantBrown
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VibrantSurfaceWarm)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
fun TrackBadge(track: Track) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(track.accentColor().copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = track.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = track.accentColor(),
        )
    }
}

@Composable
fun LevelBadge(level: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VibrantSurfaceWarm)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = level,
            style = MaterialTheme.typography.labelSmall,
            color = VibrantMuted,
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
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) IngOrange else VibrantMuted,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(96.dp)
                .background(if (isSelected) IngOrange else VibrantBorder),
        )
    }
}

@Composable
fun TimeSlotHeader(time: String) {
    Text(
        text = displayClock(time),
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        color = VibrantMuted,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun StarToggle(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (isSelected) VibrantBrown else VibrantMuted,
        label = "star",
    )
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = "Favorite",
            tint = color,
        )
    }
}

@Composable
fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFD8D8))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(GoogleRed),
        )
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = GoogleRed,
        )
    }
}

@Composable
fun CapacityBar(
    registered: Int,
    capacity: Int,
    trackColor: Color,
) {
    val fraction = (registered.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(VibrantSurfaceWarm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(trackColor),
        )
    }
}

@Composable
fun SpeakerAvatar(
    initials: String,
    color: Color,
    size: Int = 48,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.95f), Color(0xFF151515)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = (size / 3).sp,
        )
    }
}

@Composable
private fun MiniSpeakerAvatar(seed: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF727272), Color(0xFF111111)),
                )
            )
            .border(1.dp, VibrantBorder, RoundedCornerShape(9.dp)),
    )
}

fun Track.accentColor(): Color = when (this) {
    Track.AI_ML -> TrackAiMl
    Track.ANDROID -> TrackAndroid
    Track.IOS -> Color(0xFF111111)
    Track.GENERIC -> Color(0xFF5F6368)
    Track.WEB -> TrackWeb
    Track.CLOUD -> TrackCloud
    Track.FIREBASE -> TrackFirebase
    Track.FLUTTER -> TrackFlutter
    Track.DESIGN -> TrackDesign
}

fun Track.bgColor(): Color = accentColor().copy(alpha = 0.12f)

fun displayClock(iso: String): String {
    val raw = iso.substringAfter("T", iso).take(5)
    val hour = raw.take(2).toIntOrNull() ?: return iso
    val minute = raw.drop(3).take(2).ifBlank { "00" }
    val suffix = if (hour >= 12) "PM" else "AM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "${hour12.toString().padStart(2, '0')}:$minute $suffix"
}

fun displayTimeRange(session: Session): String {
    return "${displayClock(session.startTime)} - ${displayClock(session.endTime)}"
}

fun displayShortDate(iso: String): String {
    val date = iso.take(10)
    val month = date.substringAfter("-", "").take(2).toIntOrNull()
    val day = date.takeLast(2).toIntOrNull()
    val monthName = when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
    return if (day == null) date else "$monthName $day"
}

fun initials(name: String): String {
    return name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2)
}

fun avatarColor(seed: String): Color {
    val colors = listOf(IngOrange, TrackDesign, TrackAndroid, TrackAiMl, VibrantBrown)
    val index = seed.hashCode().mod(colors.size).let { if (it < 0) -it else it }
    return colors[index]
}
