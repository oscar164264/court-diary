package com.courtdiary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.courtdiary.ui.theme.*
import com.courtdiary.viewmodel.CaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: CaseViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val stats by viewModel.statistics.collectAsState()
    val allCases by viewModel.allCases.collectAsState()

    val statusColors = mapOf(
        "Active" to SafeGreen,
        "Adjourned" to WarningOrange,
        "Disposed" to PrimaryBlue,
        "Withdrawn" to UrgentRed
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Statistics", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Overview row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Cases",
                    count = stats.totalCases,
                    color = PrimaryBlue,
                    icon = Icons.Filled.Gavel
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Active",
                    count = stats.byStatus["Active"] ?: 0,
                    color = SafeGreen,
                    icon = Icons.Filled.CheckCircle
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Disposed",
                    count = stats.byStatus["Disposed"] ?: 0,
                    color = WarningOrange,
                    icon = Icons.Filled.Done
                )
            }

            // ── Cases by Status
            StatsSectionCard(title = "Cases by Status") {
                val total = stats.totalCases.coerceAtLeast(1)
                listOf("Active", "Adjourned", "Disposed", "Withdrawn").forEach { status ->
                    val count = stats.byStatus[status] ?: 0
                    val fraction = count.toFloat() / total
                    val color = statusColors[status] ?: PrimaryBlue
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(status, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(color.copy(alpha = 0.15f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                            )
                        }
                    }
                }
            }

            // ── Top Courts
            if (stats.topCourts.isNotEmpty()) {
                StatsSectionCard(title = "Most Active Courts") {
                    stats.topCourts.forEachIndexed { index, (court, count) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PrimaryBlue,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        (index + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                court,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "$count case${if (count != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index < stats.topCourts.lastIndex)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            // ── Cases filed per month
            if (stats.byMonth.isNotEmpty()) {
                StatsSectionCard(title = "Cases Filed per Month") {
                    val sortedMonths = stats.byMonth.entries
                        .sortedByDescending { it.key }
                        .take(6)
                        .reversed()
                    val maxCount = sortedMonths.maxOf { it.value }.coerceAtLeast(1)
                    sortedMonths.forEach { (month, count) ->
                        val fraction = count.toFloat() / maxCount
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatMonth(month),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    count.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(PrimaryBlue.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(50))
                                        .background(PrimaryBlue)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatMonth(yyyyMM: String): String {
    return try {
        val parts = yyyyMM.split("-")
        val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "${months[parts[1].toInt()]} ${parts[0]}"
    } catch (e: Exception) { yyyyMM }
}

@Composable
private fun StatsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = PrimaryBlue.copy(alpha = 0.2f))
            content()
        }
    }
}
