package com.courtdiary.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.courtdiary.model.CourtCase
import com.courtdiary.ui.theme.*
import com.courtdiary.utils.isWithinDays
import com.courtdiary.utils.toDisplayDate
import com.courtdiary.utils.toDisplayDateOrDash
import com.courtdiary.viewmodel.CaseResult
import com.courtdiary.viewmodel.CaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    viewModel: CaseViewModel,
    caseId: Int,
    onNavigateBack: () -> Unit,
    onEditCase: () -> Unit
) {
    val context = LocalContext.current
    var case by remember { mutableStateOf<CourtCase?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load case
    LaunchedEffect(caseId) {
        case = viewModel.getCaseById(caseId)
    }

    // Listen for delete result
    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { result ->
            when (result) {
                is CaseResult.Success -> onNavigateBack()
                is CaseResult.Error -> snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Case") },
            text = { Text("Are you sure you want to delete case ${case?.caseNumber}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        case?.let { viewModel.deleteCase(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = UrgentRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        case?.caseNumber ?: "Case Detail",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onEditCase) {
                        Icon(Icons.Filled.Edit, "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        case?.let { c ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Urgency banner
                if (c.nextHearingDate.isWithinDays(3)) {
                    val isToday = c.nextHearingDate.isWithinDays(0)
                    val color = if (isToday) UrgentRed else WarningOrange
                    val label = if (isToday) "HEARING TODAY" else "URGENT – Within 3 Days"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = color)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── CourtCase Information Card
                DetailCard(title = "Case Information") {
                    DetailRow(Icons.Filled.Tag, "Case Number", c.caseNumber)
                    DetailRow(Icons.Filled.AccountBalance, "Court", c.courtName.ifBlank { "—" })
                }

                // ── Client Card
                DetailCard(title = "Client Information") {
                    DetailRow(Icons.Filled.Person, "Name", c.clientName.ifBlank { "—" })
                    DetailRow(Icons.Filled.Phone, "Phone", c.clientPhone)

                    Spacer(Modifier.height(8.dp))
                    // Action buttons
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Call button
                        Button(
                            onClick = { dialPhone(context, c.clientPhone) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                        ) {
                            Icon(Icons.Filled.Phone, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call Client")
                        }

                        // WhatsApp button
                        Button(
                            onClick = { openWhatsApp(context, c.clientPhone) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366) // WhatsApp green
                            )
                        ) {
                            Icon(Icons.Filled.Chat, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp")
                        }
                    }
                }

                // ── Hearing Dates Card
                DetailCard(title = "Hearing Dates") {
                    DetailRow(
                        Icons.Filled.EventAvailable,
                        "Last Hearing",
                        c.lastHearingDate.toDisplayDateOrDash()
                    )
                    DetailRow(
                        Icons.Filled.EventNote,
                        "Next Hearing",
                        c.nextHearingDate.toDisplayDate(),
                        valueColor = when {
                            c.nextHearingDate.isWithinDays(3) -> UrgentRed
                            c.nextHearingDate.isWithinDays(7) -> WarningOrange
                            else -> null
                        }
                    )
                }

                // ── Stages Card
                DetailCard(title = "Case Stages") {
                    DetailRow(Icons.Filled.History, "Last Stage", c.lastStage.ifBlank { "—" })
                    DetailRow(Icons.Filled.NavigateNext, "Next Stage", c.nextStage.ifBlank { "—" })
                }

                // ── Notes Card
                if (c.notes.isNotBlank()) {
                    DetailCard(title = "Notes") {
                        Text(
                            text = c.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun DetailCard(
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
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                color = PrimaryBlue.copy(alpha = 0.2f)
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon, null,
            Modifier
                .size(18.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ──────────────────────────────────────────
// Intent helpers
// ──────────────────────────────────────────

/** Opens the phone dialer with the given number pre-filled. */
private fun dialPhone(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:${Uri.encode(phone)}")
    }
    context.startActivity(intent)
}

/**
 * Opens WhatsApp with the phone number.
 * Strips non-numeric characters (except leading +) for the wa.me deep link.
 */
private fun openWhatsApp(context: Context, phone: String) {
    val cleaned = phone.replace(Regex("[^0-9+]"), "")
        .removePrefix("+") // wa.me does not want the '+'
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://wa.me/$cleaned")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // WhatsApp not installed – open in browser as fallback
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleaned"))
        )
    }
}
