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
import androidx.core.content.FileProvider
import com.courtdiary.ui.theme.PrimaryBlue
import com.courtdiary.viewmodel.CaseResult
import com.courtdiary.viewmodel.CaseViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: CaseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    var isExporting by remember { mutableStateOf(false) }

    // File picker launcher for importing JSON
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                    viewModel.importFromJson(json)
                    Toast.makeText(context, "Import started…", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to read file: ${e.message}")
                }
            }
        }
    }

    // Listen for operation results
    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { result ->
            when (result) {
                is CaseResult.Success ->
                    snackbarHostState.showSnackbar("Operation completed successfully")
                is CaseResult.Error ->
                    snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Preferences Section
            SettingsSectionLabel("Preferences")

            SettingsCard {
                // Notifications toggle
                SettingsToggleRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = "Daily reminders for upcoming hearings",
                    checked = notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )

                HorizontalDivider(Modifier.padding(horizontal = 8.dp))

                // Dark mode toggle
                SettingsToggleRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Switch to dark theme",
                    checked = darkModeEnabled,
                    onCheckedChange = viewModel::setDarkModeEnabled
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Backup & Restore Section
            SettingsSectionLabel("Backup & Restore")

            SettingsCard {
                // Export
                SettingsActionRow(
                    icon = Icons.Filled.Upload,
                    title = "Export to JSON",
                    subtitle = "Save all cases as a backup file",
                    isLoading = isExporting,
                    onClick = {
                        scope.launch {
                            isExporting = true
                            try {
                                val file = viewModel.exportToJson()
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "Court Diary Backup"
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share Backup")
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Export failed: ${e.message}")
                            } finally {
                                isExporting = false
                            }
                        }
                    }
                )

                HorizontalDivider(Modifier.padding(horizontal = 8.dp))

                // Import
                SettingsActionRow(
                    icon = Icons.Filled.Download,
                    title = "Import from JSON",
                    subtitle = "Restore cases from a backup file",
                    onClick = { importLauncher.launch("application/json") }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── About Section
            SettingsSectionLabel("About")

            SettingsCard {
                SettingsInfoRow(
                    icon = Icons.Filled.Info,
                    title = "App Version",
                    value = "1.0.0"
                )
                HorizontalDivider(Modifier.padding(horizontal = 8.dp))
                SettingsInfoRow(
                    icon = Icons.Filled.Gavel,
                    title = "App Name",
                    value = "Court Diary"
                )
                HorizontalDivider(Modifier.padding(horizontal = 8.dp))
                SettingsInfoRow(
                    icon = Icons.Filled.Security,
                    title = "Privacy",
                    value = "All data stored locally"
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlue,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = PrimaryBlue)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = PrimaryBlue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Filled.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = PrimaryBlue)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
