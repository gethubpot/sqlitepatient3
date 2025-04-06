package com.example.sqlitepatient3.presentation.screens.diagnosis // Adjust package if needed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.domain.model.DiagnosticCode
import com.example.sqlitepatient3.presentation.components.AddFab
import com.example.sqlitepatient3.presentation.components.ConfirmationDialog
import com.example.sqlitepatient3.presentation.components.EmptyStateScaffold
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticCodeListScreen(
    onNavigateUp: () -> Unit,
    onCodeClick: (Long?) -> Unit, // Navigate to Add/Edit screen (Long? for optional ID)
    viewModel: DiagnosticCodeListViewModel = hiltViewModel()
) {
    val codes by viewModel.diagnosticCodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSortOption by viewModel.currentSortOption.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSortDialog by remember { mutableStateOf(false) }
    var codeToDelete by remember { mutableStateOf<DiagnosticCode?>(null) } // For delete confirmation

    // Show messages from ViewModel
    LaunchedEffect(message) {
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearMessage()
            }
        }
    }

    // --- Loading State ---
    if (isLoading && codes.isEmpty()) {
        LoadingScaffold(title = "Diagnostic Codes", onNavigateUp = onNavigateUp)
        return
    }

    // --- Empty State ---
    if (codes.isEmpty() && !isLoading && searchQuery.isBlank()) {
        EmptyStateScaffold(
            title = "Diagnostic Codes",
            emptyMessage = "No diagnostic codes found in your library. Add codes to get started.",
            onNavigateUp = onNavigateUp,
            onAddClick = { onCodeClick(null) } // Navigate to add screen
        )
        return
    }

    // --- Main Content ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Codes (${codes.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Optionally add search icon here if search bar is hidden initially
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(Icons.Default.Sort, "Sort")
                    }
                    // Add other actions like Filter if needed later
                }
            )
        },
        floatingActionButton = {
            AddFab(onClick = { onCodeClick(null) }) // Navigate to add screen
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by code or description...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            // List of Codes
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(codes, key = { it.id }) { code ->
                    DiagnosticCodeListItem(
                        code = code,
                        onEditClick = { onCodeClick(code.id) }, // Navigate to edit screen
                        onDeleteClick = { codeToDelete = code }, // Show delete dialog
                        onToggleCommonClick = { viewModel.toggleCommonCode(code) }
                    )
                    Divider()
                }
            }
        }
    }

    // --- Sort Dialog ---
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Codes") },
            text = {
                Column {
                    // Add Radio buttons for each sort option
                    SortDialogRadioButton(
                        text = "Code (Ascending)",
                        selected = currentSortOption == DiagnosticCodeListViewModel.SortOption.CODE_ASC,
                        onClick = { viewModel.setSortOption(DiagnosticCodeListViewModel.SortOption.CODE_ASC); showSortDialog = false }
                    )
                    SortDialogRadioButton(
                        text = "Code (Descending)",
                        selected = currentSortOption == DiagnosticCodeListViewModel.SortOption.CODE_DESC,
                        onClick = { viewModel.setSortOption(DiagnosticCodeListViewModel.SortOption.CODE_DESC); showSortDialog = false }
                    )
                    SortDialogRadioButton(
                        text = "Description (Ascending)",
                        selected = currentSortOption == DiagnosticCodeListViewModel.SortOption.DESCRIPTION_ASC,
                        onClick = { viewModel.setSortOption(DiagnosticCodeListViewModel.SortOption.DESCRIPTION_ASC); showSortDialog = false }
                    )
                    SortDialogRadioButton(
                        text = "Description (Descending)",
                        selected = currentSortOption == DiagnosticCodeListViewModel.SortOption.DESCRIPTION_DESC,
                        onClick = { viewModel.setSortOption(DiagnosticCodeListViewModel.SortOption.DESCRIPTION_DESC); showSortDialog = false }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel") // Changed to Cancel as selection closes dialog
                }
            }
        )
    }

    // --- Delete Confirmation Dialog ---
    codeToDelete?.let { code ->
        ConfirmationDialog(
            title = "Delete Code?",
            message = "Are you sure you want to delete ICD-10 code '${code.icdCode} - ${code.description}'? This cannot be undone.",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteCode(code) },
            onDismiss = { codeToDelete = null }
        )
    }
}

@Composable
private fun DiagnosticCodeListItem(
    code: DiagnosticCode,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleCommonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick) // Click row to edit
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = code.icdCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = code.description.ifBlank { "(No description)" }, // Show placeholder if blank
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (code.description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Action Icons
        // Common Code Toggle (Star Icon)
        IconButton(onClick = onToggleCommonClick) {
            Icon(
                imageVector = if (code.commonCode != null && code.commonCode > 0) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = if (code.commonCode != null && code.commonCode > 0) "Mark as not common" else "Mark as common",
                tint = if (code.commonCode != null && code.commonCode > 0) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.6f) // Use a float value like 0.6f
            )
        }
        // Delete Icon
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Code",
                tint = MaterialTheme.colorScheme.error
            )
        }
        // Edit Icon (optional, since row click edits)
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Code"
            )
        }
    }
}

// Helper for Radio Button Rows in Dialogs (identical to the one in PatientListScreen)
@Composable
private fun SortDialogRadioButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}