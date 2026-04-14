package com.courtdiary.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.courtdiary.model.CaseDocument
import com.courtdiary.model.CourtCase
import com.courtdiary.model.FeeEntry
import com.courtdiary.model.HearingEntry
import com.courtdiary.ui.theme.*
import com.courtdiary.utils.FileUtils
import com.courtdiary.utils.isWithinDays
import com.courtdiary.utils.toDisplayDate
import com.courtdiary.utils.toDisplayDateOrDash
import com.courtdiary.viewmodel.CaseResult
import com.courtdiary.viewmodel.CaseViewModel
import java.io.File

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
    var isAttaching by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val documents by viewModel.getDocumentsForCase(caseId).collectAsState(initial = emptyList())
    val hearingEntries by viewModel.getHearingEntriesForCase(caseId).collectAsState(initial = emptyList())
    val feeEntries by viewModel.getFeeEntriesForCase(caseId).collectAsState(initial = emptyList())
    val totalCharged by viewModel.getTotalCharged(caseId).collectAsState(initial = 0.0)
    val totalPaid by viewModel.getTotalPaid(caseId).collectAsState(initial = 0.0)

    var showAddHearingDialog by remember { mutableStateOf(false) }
    var showAddFeeDialog by remember { mutableStateOf(false) }

    // ── Add Hearing Dialog
    if (showAddHearingDialog) {
        AddHearingDialog(
            caseId = caseId,
            onDismiss = { showAddHearingDialog = false },
            onConfirm = { entry ->
                viewModel.addHearingEntry(entry)
                showAddHearingDialog = false
            }
        )
    }

    // ── Add Fee Dialog
    if (showAddFeeDialog) {
        AddFeeDialog(
            caseId = caseId,
            onDismiss = { showAddFeeDialog = false },
            onConfirm = { entry ->
                viewModel.addFeeEntry(entry)
                showAddFeeDialog = false
            }
        )
    }

    val pickDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isAttaching = true
            viewModel.attachDocument(caseId, it) { success ->
                isAttaching = false
                if (!success) Toast.makeText(context, "Failed to attach document", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    val statusColor = when (c.status) {
                        "Active" -> SafeGreen
                        "Adjourned" -> WarningOrange
                        "Disposed" -> PrimaryBlue
                        "Withdrawn" -> UrgentRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    DetailRow(Icons.Filled.Flag, "Status", c.status, valueColor = statusColor)
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

                // ── Opponent Card
                if (c.opposingParty.isNotBlank() || c.opposingCounsel.isNotBlank()) {
                    DetailCard(title = "Opponent Information") {
                        DetailRow(Icons.Filled.People, "Opposing Party", c.opposingParty.ifBlank { "—" })
                        DetailRow(Icons.Filled.Person, "Opposing Counsel", c.opposingCounsel.ifBlank { "—" })
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

                // ── Hearing History Card
                DetailCard(title = "Hearing History") {
                    if (hearingEntries.isEmpty()) {
                        Text(
                            "No hearing records yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        hearingEntries.forEach { entry ->
                            HearingEntryRow(
                                entry = entry,
                                onDelete = { viewModel.deleteHearingEntry(entry) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddHearingDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log Hearing")
                    }
                }

                // ── Fee Tracking Card
                DetailCard(title = "Fee Tracking") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = SafeGreen.copy(alpha = 0.12f)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("Charged", style = MaterialTheme.typography.labelSmall, color = SafeGreen)
                                Text(
                                    "৳ %.2f".format(totalCharged),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeGreen
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("Paid", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                                Text(
                                    "৳ %.2f".format(totalPaid),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                        val outstanding = totalCharged - totalPaid
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = (if (outstanding > 0) UrgentRed else SafeGreen).copy(alpha = 0.12f)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("Due", style = MaterialTheme.typography.labelSmall,
                                    color = if (outstanding > 0) UrgentRed else SafeGreen)
                                Text(
                                    "৳ %.2f".format(outstanding),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (outstanding > 0) UrgentRed else SafeGreen
                                )
                            }
                        }
                    }

                    if (feeEntries.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        feeEntries.forEach { entry ->
                            FeeEntryRow(
                                entry = entry,
                                onDelete = { viewModel.deleteFeeEntry(entry) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddFeeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Fee Entry")
                    }
                }

                // ── Documents Card
                DetailCard(title = "Documents") {
                    if (documents.isEmpty() && !isAttaching) {
                        Text(
                            "No documents attached yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    documents.forEach { doc ->
                        DocumentRow(
                            doc = doc,
                            onOpen = { openDocument(context, doc) },
                            onDelete = { viewModel.deleteDocument(doc) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (isAttaching) {
                        Row(
                            Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Attaching…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            pickDocument.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/*"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AttachFile, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Attach Document (PDF / Word / Image)")
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
// Document row
// ──────────────────────────────────────────

@Composable
private fun DocumentRow(
    doc: CaseDocument,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Document") },
            text = { Text("Remove \"${doc.fileName}\" from this case?") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = UrgentRed)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val icon = when {
        doc.mimeType.startsWith("image/") -> Icons.Filled.Image
        doc.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
        doc.mimeType.contains("word") || doc.mimeType.contains("document") -> Icons.Filled.Article
        else -> Icons.Filled.AttachFile
    }
    val iconTint = when {
        doc.mimeType.startsWith("image/") -> Color(0xFF4CAF50)
        doc.mimeType == "application/pdf" -> Color(0xFFE53935)
        doc.mimeType.contains("word") || doc.mimeType.contains("document") -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = iconTint)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                doc.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                FileUtils.formatFileSize(doc.fileSize),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Filled.OpenInNew, "Open", tint = PrimaryBlue)
        }
        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(Icons.Filled.Delete, "Remove", tint = UrgentRed)
        }
    }
}

// ──────────────────────────────────────────
// Intent helpers
// ──────────────────────────────────────────

/** Opens the file with the appropriate app installed on the device. */
private fun openDocument(context: Context, doc: CaseDocument) {
    val file = File(doc.filePath)
    if (!file.exists()) {
        Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileUtils.getFileProviderUri(context, file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, doc.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}

/** Opens the phone dialer with the given number pre-filled. */
private fun dialPhone(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:${Uri.encode(phone)}")
    }
    context.startActivity(intent)
}

// ──────────────────────────────────────────
// Hearing entry row
// ──────────────────────────────────────────

@Composable
private fun HearingEntryRow(entry: HearingEntry, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Remove Entry") },
            text = { Text("Remove this hearing record?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = UrgentRed)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.Gavel, null, Modifier.size(18.dp).padding(top = 2.dp),
            tint = PrimaryBlue)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.date.toDisplayDate(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (entry.stage.isNotBlank()) Text("Stage: ${entry.stage}", style = MaterialTheme.typography.bodySmall)
            if (entry.outcome.isNotBlank()) Text("Outcome: ${entry.outcome}", style = MaterialTheme.typography.bodySmall)
            if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { showDelete = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, null, tint = UrgentRed, modifier = Modifier.size(18.dp))
        }
    }
}

// ──────────────────────────────────────────
// Fee entry row
// ──────────────────────────────────────────

@Composable
private fun FeeEntryRow(entry: FeeEntry, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Remove Entry") },
            text = { Text("Remove this fee record?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = UrgentRed)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
    val isCharged = entry.type == "CHARGED"
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isCharged) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            null, Modifier.size(18.dp),
            tint = if (isCharged) WarningOrange else SafeGreen
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${if (isCharged) "Charged" else "Paid"}  ৳ %.2f".format(entry.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isCharged) WarningOrange else SafeGreen
            )
            if (entry.description.isNotBlank())
                Text(entry.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(entry.date.toDisplayDate(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { showDelete = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, null, tint = UrgentRed, modifier = Modifier.size(18.dp))
        }
    }
}

// ──────────────────────────────────────────
// Add Hearing Dialog
// ──────────────────────────────────────────

private val OUTCOME_OPTIONS = listOf(
    "Adjourned", "Part Heard", "Arguments Heard", "Judgment Reserved",
    "Disposed", "Withdrawn", "Witness Examined", "Documents Submitted"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHearingDialog(
    caseId: Int,
    onDismiss: () -> Unit,
    onConfirm: (HearingEntry) -> Unit
) {
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var stage by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var outcomeExpanded by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Hearing", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Date
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarToday, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(date.toDisplayDate())
                }
                // Stage
                OutlinedTextField(
                    value = stage,
                    onValueChange = { stage = it },
                    label = { Text("Stage") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Outcome dropdown
                ExposedDropdownMenuBox(
                    expanded = outcomeExpanded,
                    onExpandedChange = { outcomeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = outcome,
                        onValueChange = { outcome = it },
                        label = { Text("Outcome") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(outcomeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = outcomeExpanded,
                        onDismissRequest = { outcomeExpanded = false }
                    ) {
                        OUTCOME_OPTIONS.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                outcome = opt; outcomeExpanded = false
                            })
                        }
                    }
                }
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(HearingEntry(caseId = caseId, date = date, stage = stage.trim(),
                    outcome = outcome.trim(), notes = notes.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ──────────────────────────────────────────
// Add Fee Dialog
// ──────────────────────────────────────────

@Composable
private fun AddFeeDialog(
    caseId: Int,
    onDismiss: () -> Unit,
    onConfirm: (FeeEntry) -> Unit
) {
    var type by remember { mutableStateOf("CHARGED") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Fee Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CHARGED", "PAID").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; amountError = false },
                    label = { Text("Amount (৳)") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (parsed == null || parsed <= 0) {
                    amountError = true
                    return@TextButton
                }
                onConfirm(FeeEntry(caseId = caseId, type = type, amount = parsed,
                    description = description.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Opens WhatsApp with the phone number.
 * Strips non-numeric characters (except leading +) for the wa.me deep link.
 */
private fun openWhatsApp(context: Context, phone: String) {
    var cleaned = phone.replace(Regex("[^0-9+]"), "")
        .removePrefix("+") // wa.me does not want the '+'
    if (!cleaned.startsWith("880")) {
        cleaned = "880$cleaned"
    }
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
