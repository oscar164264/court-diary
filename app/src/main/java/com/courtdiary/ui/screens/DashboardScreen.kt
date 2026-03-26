package com.courtdiary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.courtdiary.model.CourtCase
import com.courtdiary.ui.theme.*
import com.courtdiary.utils.isWithinDays
import com.courtdiary.utils.toDisplayDate
import com.courtdiary.viewmodel.CaseViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CaseViewModel,
    onCaseClick: (Int) -> Unit
) {
    val todayCases by viewModel.todayCases.collectAsState()
    val upcomingCases by viewModel.upcomingCases.collectAsState()

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Court Diary",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = today.format(dateFormatter),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Stats row
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Today",
                        count = todayCases.size,
                        color = PrimaryBlue,
                        icon = Icons.Filled.Today
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "This Week",
                        count = upcomingCases.size,
                        color = WarningOrange,
                        icon = Icons.Filled.CalendarMonth
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Urgent",
                        count = upcomingCases.count { it.nextHearingDate.isWithinDays(3) },
                        color = UrgentRed,
                        icon = Icons.Filled.Warning
                    )
                }
            }

            // ── Today's cases
            item {
                SectionHeader(title = "Today's Hearings", count = todayCases.size)
            }

            if (todayCases.isEmpty()) {
                item { EmptyStateSmall(message = "No cases scheduled for today") }
            } else {
                items(todayCases, key = { it.id }) { case ->
                    CaseCard(case = case, onClick = { onCaseClick(case.id) })
                }
            }

            // ── Upcoming cases
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(title = "Upcoming (Next 7 Days)", count = upcomingCases.size)
            }

            if (upcomingCases.isEmpty()) {
                item { EmptyStateSmall(message = "No upcoming cases this week") }
            } else {
                items(upcomingCases, key = { "up_${it.id}" }) { case ->
                    CaseCard(case = case, onClick = { onCaseClick(case.id) })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ──────────────────────────────────────────
// Shared composables used by multiple screens
// ──────────────────────────────────────────

@Composable
fun CaseCard(
    case: CourtCase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine urgency colour
    val isUrgent = case.nextHearingDate.isWithinDays(3)
    val isVeryUrgent = case.nextHearingDate.isWithinDays(1)

    val accentColor = when {
        isVeryUrgent -> UrgentRed
        isUrgent -> WarningOrange
        else -> SafeGreen
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            // Left colour indicator strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CourtCase number badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = case.caseNumber,
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Urgency chip
                    if (isUrgent) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isVeryUrgent) "TODAY" else "URGENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                if (case.clientName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Person, null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = case.clientName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }

                if (case.courtName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccountBalance, null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = case.courtName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Event, null,
                        Modifier.size(14.dp),
                        tint = accentColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Next: ${case.nextHearingDate.toDisplayDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (case.nextStage.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Stage: ${case.nextStage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Filled.ChevronRight, null,
                Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = PrimaryBlue
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = color)
            Spacer(Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun EmptyStateSmall(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

