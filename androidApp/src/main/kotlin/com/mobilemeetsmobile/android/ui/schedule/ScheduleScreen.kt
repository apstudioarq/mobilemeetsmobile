package com.mobilemeetsmobile.android.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilemeetsmobile.android.ui.components.DayTab
import com.mobilemeetsmobile.android.ui.components.SessionCard
import com.mobilemeetsmobile.android.ui.components.TimeSlotHeader
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantText
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onSessionClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Mobile Meets\nMobile",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 46.sp, lineHeight = 52.sp),
                color = VibrantText,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "The premier gathering for mobile innovators.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = VibrantMuted,
            )
            Spacer(Modifier.height(42.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.days) { day ->
                    DayTab(
                        label = day.label,
                        date = day.date,
                        isSelected = state.selectedDay == day.dayNumber,
                        onClick = { viewModel.loadDay(day.dayNumber) },
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        if (state.isLoading && state.sessions.isEmpty()) {
            item {
                Row(Modifier.padding(40.dp)) {
                    CircularProgressIndicator(color = IngOrange)
                }
            }
        } else {
            val sortedSlots = state.timeSlots.entries.sortedBy { it.key }
            sortedSlots.forEach { (time, sessions) ->
                item(key = "time_$time") {
                    TimeSlotHeader(time = time)
                }
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.id) },
                        onBookmarkClick = { viewModel.onBookmarkToggle(session.id) },
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(72.dp))
        }
    }
}
