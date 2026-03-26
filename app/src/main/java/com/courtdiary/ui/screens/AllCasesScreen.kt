package com.courtdiary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.courtdiary.model.Case
import com.courtdiary.ui.theme.PrimaryBlue
import com.courtdiary.viewmodel.CaseFilter
import com.courtdiary.viewmodel.CaseViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCasesScreen(
    viewModel: CaseViewModel,
    onCaseClick: (Int) -> Unit,
    onAddCase: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val allCases by viewModel.allCases.collectAsState()
    val todayCases by viewModel.todayCases.collectAsState()
    val upcomingCases by viewModel.upcomingCases.collectAsState()
    val pastCases by viewModel.pastCases.collectAsState()

    // Determine which list to show
    val displayCases: List<Case> = when {
        searchQuery.isNotBlank() -> searchResults
        activeFilter == CaseFilter.TODAY -> todayCases
        activeFilter == CaseFilter.UPCOMING -> upcomingCases
        activeFilter == CaseFilter.PAST -> pastCases
        else -> allCases
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "All Cases",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
                )
                // Search bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCase,
                containerColor = PrimaryBlue
            ) {
                Icon(Icons.Filled.Add, "Add Case", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips (only visible when not searching)
            AnimatedVisibility(visible = searchQuery.isBlank()) {
                FilterChipRow(
                    activeFilter = activeFilter,
                    onFilterSelected = viewModel::onFilterChanged,
                    allCount = allCases.size,
                    todayCount = todayCases.size,
                    upcomingCount = upcomingCases.size,
                    pastCount = pastCases.size
                )
            }

            if (displayCases.isEmpty()) {
                EmptyState(
                    message = if (searchQuery.isNotBlank())
                        "No results for \"$searchQuery\""
                    else
                        "No cases found"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${displayCases.size} case${if (displayCases.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(displayCases, key = { it.id }) { case ->
                        CaseCard(case = case, onClick = { onCaseClick(case.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search by case number, client, phone…") },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, "Clear search")
                }
            }
        },
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun FilterChipRow(
    activeFilter: CaseFilter,
    onFilterSelected: (CaseFilter) -> Unit,
    allCount: Int,
    todayCount: Int,
    upcomingCount: Int,
    pastCount: Int
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeFilter == CaseFilter.ALL,
            onClick = { onFilterSelected(CaseFilter.ALL) },
            label = { Text("All ($allCount)") }
        )
        FilterChip(
            selected = activeFilter == CaseFilter.TODAY,
            onClick = { onFilterSelected(CaseFilter.TODAY) },
            label = { Text("Today ($todayCount)") }
        )
        FilterChip(
            selected = activeFilter == CaseFilter.UPCOMING,
            onClick = { onFilterSelected(CaseFilter.UPCOMING) },
            label = { Text("Upcoming ($upcomingCount)") }
        )
        FilterChip(
            selected = activeFilter == CaseFilter.PAST,
            onClick = { onFilterSelected(CaseFilter.PAST) },
            label = { Text("Past ($pastCount)") }
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.FolderOpen,
            null,
            Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
