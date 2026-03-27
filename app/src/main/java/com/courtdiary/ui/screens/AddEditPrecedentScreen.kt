package com.courtdiary.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.courtdiary.model.Precedent
import com.courtdiary.ui.theme.PrimaryBlue
import com.courtdiary.utils.FileUtils
import com.courtdiary.viewmodel.PrecedentResult
import com.courtdiary.viewmodel.PrecedentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPrecedentScreen(
    viewModel: PrecedentViewModel,
    precedentId: Int?,          // null = add, non-null = edit
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = precedentId != null

    var existing by remember { mutableStateOf<Precedent?>(null) }
    LaunchedEffect(precedentId) {
        if (precedentId != null) existing = viewModel.getPrecedentById(precedentId)
    }

    // Form fields
    var title by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    var pickedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pickedFileName by remember { mutableStateOf("") }

    // Populate on edit
    LaunchedEffect(existing) {
        existing?.let {
            title = it.title
            textContent = it.textContent
            if (it.fileName.isNotBlank()) pickedFileName = it.fileName
        }
    }

    // Validation
    var titleError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for save result
    LaunchedEffect(Unit) {
        viewModel.result.collect { result ->
            when (result) {
                is PrecedentResult.Success -> onNavigateBack()
                is PrecedentResult.Error -> {
                    isSaving = false
                    snackbarHostState.showSnackbar(result.message)
                }
            }
        }
    }

    // File picker
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pickedFileUri = it
            // Try to get the filename to display
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) pickedFileName = cursor.getString(idx)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Precedent" else "Add Precedent",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = null },
                label = { Text("Title *") },
                leadingIcon = { Icon(Icons.Filled.Title, null) },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Text content
            Text(
                "Paste or type legal text",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.3f))

            OutlinedTextField(
                value = textContent,
                onValueChange = { textContent = it },
                label = { Text("Legal text / precedent content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                maxLines = 20,
                placeholder = { Text("Paste the legal precedent text here…") }
            )

            // File upload
            Text(
                "Or attach a file (PDF / Word)",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.3f))

            OutlinedButton(
                onClick = {
                    pickFile.launch(
                        arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.UploadFile, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (pickedFileName.isBlank()) "Choose PDF or Word file" else "Change file")
            }

            // Show picked / existing file
            val displayFile = when {
                pickedFileName.isNotBlank() -> pickedFileName
                isEditMode && existing?.fileName?.isNotBlank() == true -> existing!!.fileName
                else -> null
            }
            if (displayFile != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.InsertDriveFile, null, tint = PrimaryBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            displayFile,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (pickedFileUri != null) {
                            IconButton(onClick = {
                                pickedFileUri = null
                                pickedFileName = existing?.fileName ?: ""
                            }) {
                                Icon(Icons.Filled.Close, "Remove new file", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Save button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Title is required"
                        return@Button
                    }
                    if (textContent.isBlank() && pickedFileUri == null &&
                        (existing == null || existing!!.filePath.isBlank())
                    ) {
                        Toast.makeText(context, "Add text content or attach a file", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    viewModel.savePrecedent(
                        id = precedentId ?: 0,
                        title = title,
                        textContent = textContent,
                        fileUri = pickedFileUri,
                        existingFilePath = existing?.filePath ?: "",
                        existingFileName = existing?.fileName ?: "",
                        existingMimeType = existing?.mimeType ?: "",
                        existingFileSize = existing?.fileSize ?: 0L
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(if (isEditMode) Icons.Filled.Save else Icons.Filled.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEditMode) "Update Precedent" else "Save Precedent", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
