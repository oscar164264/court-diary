package com.courtdiary.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.courtdiary.model.Precedent
import com.courtdiary.ui.theme.PrimaryBlue
import com.courtdiary.ui.theme.UrgentRed
import com.courtdiary.utils.FileUtils
import com.courtdiary.viewmodel.PrecedentViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrecedentDetailScreen(
    viewModel: PrecedentViewModel,
    precedentId: Int,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    var precedent by remember { mutableStateOf<Precedent?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(precedentId) {
        precedent = viewModel.getPrecedentById(precedentId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Precedent") },
            text = { Text("Delete \"${precedent?.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        precedent?.let { viewModel.deletePrecedent(it) }
                        showDeleteDialog = false
                        onNavigateBack()
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
                        precedent?.title ?: "Precedent",
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
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        precedent?.let { p ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(Date(p.updatedAt))
                Text(
                    "Last updated: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Text content card
                if (p.textContent.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(Icons.Filled.Notes, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Legal Text",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(
                                Modifier.padding(vertical = 8.dp),
                                color = PrimaryBlue.copy(alpha = 0.2f)
                            )
                            Text(
                                p.textContent,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // File card
                if (p.filePath.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                val icon = when {
                                    p.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
                                    p.mimeType.contains("word") -> Icons.Filled.Article
                                    else -> Icons.Filled.AttachFile
                                }
                                val tint = when {
                                    p.mimeType == "application/pdf" -> Color(0xFFE53935)
                                    p.mimeType.contains("word") -> Color(0xFF1565C0)
                                    else -> PrimaryBlue
                                }
                                Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Attached File",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(
                                Modifier.padding(vertical = 8.dp),
                                color = PrimaryBlue.copy(alpha = 0.2f)
                            )
                            Text(
                                p.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (p.fileSize > 0) {
                                Text(
                                    FileUtils.formatFileSize(p.fileSize),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { openPrecedentFile(context, p) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.OpenInNew, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open File")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun openPrecedentFile(context: Context, precedent: Precedent) {
    val file = File(precedent.filePath)
    if (!file.exists()) {
        Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileUtils.getFileProviderUri(context, file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, precedent.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}
