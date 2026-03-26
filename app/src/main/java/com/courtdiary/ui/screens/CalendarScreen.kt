package com.courtdiary.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.courtdiary.ui.theme.*
import com.courtdiary.viewmodel.CaseViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CaseViewModel,
    onCaseClick: (Int) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val hearingDates by viewModel.hearingDates.collectAsState()
    val casesForDate by viewModel.casesForSelectedDate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Calendar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Calendar header
            item {
                CalendarHeader(
                    currentMonth = currentMonth,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
                )
            }

            // Day-of-week headers
            item {
                DayOfWeekRow()
            }

            // Calendar grid
            item {
                CalendarGrid(
                    yearMonth = currentMonth,
                    hearingDates = hearingDates,
                    selectedDate = selectedDate,
                    onDateClick = { date -> viewModel.selectCalendarDate(date) }
                )
            }

            // Divider
            item {
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Cases for selected date
            if (selectedDate != null) {
                item {
                    Text(
                        text = selectedDate!!.format(
                            java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (casesForDate.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cases on this date",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(casesForDate, key = { it.id }) { case ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            CaseCard(case = case, onClick = { onCaseClick(case.id) })
                        }
                    }
                }
            } else {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap a date to see scheduled cases",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Filled.ChevronLeft, "Previous month")
        }

        Text(
            text = currentMonth.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
                    + " " + currentMonth.year,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Filled.ChevronRight, "Next month")
        }
    }
}

@Composable
private fun DayOfWeekRow() {
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        dayLabels.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    hearingDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()

    // Build the list of day cells (null = empty padding cell)
    val firstDay = yearMonth.atDay(1)
    // Sunday = 0, Monday = 1, ..., Saturday = 6
    val offset = (firstDay.dayOfWeek.value % 7)
    val days = buildList {
        repeat(offset) { add(null) }
        for (d in 1..yearMonth.lengthOfMonth()) add(yearMonth.atDay(d))
        // Pad to complete last row
        while (size % 7 != 0) add(null)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                hasCase = date in hearingDates,
                                onClick = { onDateClick(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasCase: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> PrimaryBlue
        isToday -> PrimaryBlue.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> PrimaryBlue
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            // Dot indicator for dates with cases
            if (hasCase) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else UrgentRed)
                )
            }
        }
    }
}
