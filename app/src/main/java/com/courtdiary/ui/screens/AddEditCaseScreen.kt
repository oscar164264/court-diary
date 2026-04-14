package com.courtdiary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.courtdiary.model.CourtCase
import com.courtdiary.ui.theme.PrimaryBlue
import com.courtdiary.utils.toDisplayDate
import com.courtdiary.viewmodel.CaseResult
import com.courtdiary.viewmodel.CaseViewModel
import kotlinx.coroutines.launch

private val STATUS_OPTIONS = listOf("Active", "Adjourned", "Disposed", "Withdrawn")

private val STAGE_OPTIONS = listOf(
    "Filing",
    "Summons",
    "Warrant",
    "News Paper Notice",
    "SD",
    "Charge Hearing",
    "Examination In Chief",
    "Cross Examination",
    "Bail Hearing",
    "Argument",
    "342",
    "Judgment",
    "Execution",
    "Petition Hearing",
    "Document Submission",
    "Written Statement"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCaseScreen(
    viewModel: CaseViewModel,
    caseId: Int?,          // null = add mode, non-null = edit mode
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditMode = caseId != null

    // Load existing case if in edit mode
    var existingCase by remember { mutableStateOf<CourtCase?>(null) }
    LaunchedEffect(caseId) {
        if (caseId != null) {
            existingCase = viewModel.getCaseById(caseId)
        }
    }

    // Form fields
    var caseNumber by remember { mutableStateOf("") }
    var courtName by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var lastHearingDate by remember { mutableStateOf<Long?>(null) }
    var nextHearingDate by remember { mutableStateOf<Long?>(null) }
    var lastStage by remember { mutableStateOf("") }
    var nextStage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    var opposingParty by remember { mutableStateOf("") }
    var opposingCounsel by remember { mutableStateOf("") }

    // Populate form when editing
    LaunchedEffect(existingCase) {
        existingCase?.let { c ->
            caseNumber = c.caseNumber
            courtName = c.courtName
            clientName = c.clientName
            clientPhone = c.clientPhone
            lastHearingDate = c.lastHearingDate
            nextHearingDate = c.nextHearingDate
            lastStage = c.lastStage
            nextStage = c.nextStage
            notes = c.notes
            status = c.status
            opposingParty = c.opposingParty
            opposingCounsel = c.opposingCounsel
        }
    }

    // Validation errors
    var caseNumberError by remember { mutableStateOf<String?>(null) }
    var clientPhoneError by remember { mutableStateOf<String?>(null) }
    var nextHearingDateError by remember { mutableStateOf<String?>(null) }

    // DatePicker state
    var showLastDatePicker by remember { mutableStateOf(false) }
    var showNextDatePicker by remember { mutableStateOf(false) }

    // Operation result snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for results from ViewModel
    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { result ->
            when (result) {
                is CaseResult.Success -> onNavigateBack()
                is CaseResult.Error -> snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    // Date Picker Dialogs
    if (showLastDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = lastHearingDate
        )
        DatePickerDialog(
            onDismissRequest = { showLastDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    lastHearingDate = state.selectedDateMillis
                    showLastDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showLastDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showNextDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = nextHearingDate
        )
        DatePickerDialog(
            onDismissRequest = { showNextDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    nextHearingDate = state.selectedDateMillis
                    nextHearingDateError = null
                    showNextDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showNextDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Case" else "Add New Case",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Section: Case Information
            FormSectionLabel("Case Information")

            // Case Number (required)
            OutlinedTextField(
                value = caseNumber,
                onValueChange = {
                    caseNumber = it
                    caseNumberError = null
                },
                label = { Text("Case Number *") },
                leadingIcon = { Icon(Icons.Filled.Tag, null) },
                isError = caseNumberError != null,
                supportingText = caseNumberError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Court Name
            OutlinedTextField(
                value = courtName,
                onValueChange = { courtName = it },
                label = { Text("Court Name") },
                leadingIcon = { Icon(Icons.Filled.AccountBalance, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Status
            StageDropdown(
                label = "Case Status",
                value = status,
                onValueChange = { status = it },
                icon = Icons.Filled.Flag,
                options = STATUS_OPTIONS
            )

            // ── Section: Opponent Information
            Spacer(Modifier.height(4.dp))
            FormSectionLabel("Opponent Information")

            OutlinedTextField(
                value = opposingParty,
                onValueChange = { opposingParty = it },
                label = { Text("Opposing Party") },
                leadingIcon = { Icon(Icons.Filled.People, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = opposingCounsel,
                onValueChange = { opposingCounsel = it },
                label = { Text("Opposing Counsel") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Section: Client Information
            Spacer(Modifier.height(4.dp))
            FormSectionLabel("Client Information")

            // Client Name (optional)
            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("Client Name (Optional)") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Client Phone (required)
            OutlinedTextField(
                value = clientPhone,
                onValueChange = {
                    clientPhone = it
                    clientPhoneError = null
                },
                label = { Text("Client Phone Number *") },
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = clientPhoneError != null,
                supportingText = clientPhoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Section: Hearing Dates
            Spacer(Modifier.height(4.dp))
            FormSectionLabel("Hearing Dates")

            // Last Hearing Date
            DateField(
                label = "Last Hearing Date",
                value = lastHearingDate,
                onClick = { showLastDatePicker = true },
                onClear = { lastHearingDate = null }
            )

            // Next Hearing Date (required)
            DateField(
                label = "Next Hearing Date *",
                value = nextHearingDate,
                onClick = { showNextDatePicker = true },
                onClear = { nextHearingDate = null },
                isError = nextHearingDateError != null,
                errorText = nextHearingDateError
            )

            // ── Section: Case Stages
            Spacer(Modifier.height(4.dp))
            FormSectionLabel("Case Stages")

            StageDropdown(
                label = "Last Stage",
                value = lastStage,
                onValueChange = { lastStage = it },
                icon = Icons.Filled.History
            )

            StageDropdown(
                label = "Next Stage",
                value = nextStage,
                onValueChange = { nextStage = it },
                icon = Icons.Filled.NavigateNext
            )

            // ── Notes
            Spacer(Modifier.height(4.dp))
            FormSectionLabel("Notes")

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Case Details") },
                leadingIcon = { Icon(Icons.Filled.Notes, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 6
            )

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    // Validate
                    var valid = true
                    if (caseNumber.isBlank()) {
                        caseNumberError = "Case number is required"
                        valid = false
                    }
                    if (clientPhone.isBlank()) {
                        clientPhoneError = "Phone number is required"
                        valid = false
                    } else if (!clientPhone.matches(Regex("[+0-9\\s\\-()]{7,15}"))) {
                        clientPhoneError = "Enter a valid phone number"
                        valid = false
                    }
                    if (nextHearingDate == null) {
                        nextHearingDateError = "Next hearing date is required"
                        valid = false
                    }

                    if (valid) {
                        val case = CourtCase(
                            id = existingCase?.id ?: 0,
                            caseNumber = caseNumber.trim(),
                            courtName = courtName.trim(),
                            clientName = clientName.trim(),
                            clientPhone = clientPhone.trim(),
                            lastHearingDate = lastHearingDate,
                            nextHearingDate = nextHearingDate!!,
                            lastStage = lastStage.trim(),
                            nextStage = nextStage.trim(),
                            notes = notes.trim(),
                            status = status,
                            opposingParty = opposingParty.trim(),
                            opposingCounsel = opposingCounsel.trim(),
                            createdAt = existingCase?.createdAt ?: System.currentTimeMillis()
                        )
                        if (isEditMode) {
                            viewModel.updateCase(case)
                        } else {
                            viewModel.insertCase(case)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(
                    if (isEditMode) Icons.Filled.Save else Icons.Filled.Add,
                    null,
                    Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEditMode) "Update Case" else "Save Case",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlue,
        fontWeight = FontWeight.Bold
    )
    HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.3f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    isError: Boolean = false,
    errorText: String? = null
) {
    // Box overlay is required because OutlinedTextField intercepts clicks internally.
    // The transparent Box on top captures the tap and forwards it to onClick.
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.toDisplayDate() ?: "",
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
            trailingIcon = {
                if (value != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Clear, "Clear date")
                    }
                }
            },
            readOnly = true,
            isError = isError,
            supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tap to select") }
        )
        // Transparent click-capture overlay (excludes trailing icon area so Clear still works)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageDropdown(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<String> = STAGE_OPTIONS
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
