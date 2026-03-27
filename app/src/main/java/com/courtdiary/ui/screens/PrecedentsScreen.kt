package com.courtdiary.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.courtdiary.model.Precedent
import com.courtdiary.ui.theme.PrimaryBlue
import com.courtdiary.viewmodel.PrecedentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrecedentsScreen(
    viewModel: PrecedentViewModel,
    onAddPrecedent: () -> Unit,
    onPrecedentClick: (Int) -> Unit,
    onSettings: () -> Unit
) {
    val precedents by viewModel.precedents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Precedents", fontWeight = FontWeight.Bold, color = Color.White)
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPrecedent,
                containerColor = PrimaryBlue
            ) {
                Icon(Icons.Filled.Add, "Add Precedent", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("Search by title, content or filename…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (precedents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LibraryBooks,
                            null,
                            Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isBlank()) "No precedents yet.\nTap + to add one."
                            else "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(precedents, key = { it.id }) { precedent ->
                        PrecedentCard(
                            precedent = precedent,
                            onClick = { onPrecedentClick(precedent.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PrecedentCard(precedent: Precedent, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        .format(Date(precedent.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on content type
            val icon = when {
                precedent.filePath.isNotBlank() && precedent.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
                precedent.filePath.isNotBlank() && precedent.mimeType.contains("word") -> Icons.Filled.Article
                precedent.filePath.isNotBlank() -> Icons.Filled.AttachFile
                else -> Icons.Filled.Notes
            }
            val iconTint = when {
                precedent.mimeType == "application/pdf" -> Color(0xFFE53935)
                precedent.mimeType.contains("word") -> Color(0xFF1565C0)
                precedent.filePath.isNotBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> PrimaryBlue
            }
            Icon(icon, null, Modifier.size(32.dp), tint = iconTint)
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    precedent.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val subtitle = when {
                    precedent.textContent.isNotBlank() ->
                        precedent.textContent.take(80).replace('\n', ' ')
                    precedent.fileName.isNotBlank() -> "File: ${precedent.fileName}"
                    else -> "No content"
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                Icons.Filled.ChevronRight, null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
