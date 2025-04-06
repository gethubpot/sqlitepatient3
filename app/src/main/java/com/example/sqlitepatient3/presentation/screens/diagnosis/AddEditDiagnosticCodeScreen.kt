package com.example.sqlitepatient3.presentation.screens.diagnosis // Adjust package if needed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.presentation.components.ConfirmationDialog
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import com.example.sqlitepatient3.presentation.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDiagnosticCodeScreen(
    // codeId will be provided by SavedStateHandle in ViewModel
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit, // Use this to navigate back after successful save
    viewModel: AddEditDiagnosticCodeViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Form field states
    val icdCode by viewModel.icdCode.collectAsState()
    val description by viewModel.description.collectAsState()
    val shorthand by viewModel.shorthand.collectAsState()
    val isBillable by viewModel.isBillable.collectAsState()
    val isCommon by viewModel.isCommon.collectAsState()

    // Use LaunchedEffect to navigate back upon successful save
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onSaveComplete() // Call the lambda passed from NavHost
        }
    }

    // State for cancel confirmation dialog
    var showCancelDialog by remember { mutableStateOf(false) }
    // Remember initial state to check for changes (optional but good practice)
    // val initialCodeState = remember { /* capture initial state from viewModel if needed */ }
    val hasUnsavedChanges = remember(icdCode, description, shorthand, isBillable, isCommon) {
        // Basic check: If adding, any input is a change. If editing, compare to initial state.
        // For simplicity now, just check if fields are non-empty when adding.
        // A more robust check would compare against the initially loaded values if editing.
        icdCode.isNotEmpty() || description.isNotEmpty() || shorthand.isNotEmpty()
    }


    // --- Loading State ---
    if (isLoading) {
        // Use codeId from viewModel to determine title - requires exposing codeId or isEditMode
        // For now, use a generic title
        LoadingScaffold(title = "Loading Code...", onNavigateUp = onNavigateUp)
        return
    }

    // --- Main Content ---
    Scaffold(
        topBar = {
            TopAppBar(
                // Title could be dynamic based on whether codeId was passed
                title = { Text(if (icdCode.isBlank()) "Add Code" else "Edit Code") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showCancelDialog = true
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Make content scrollable
        ) {
            // Error message display
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // --- Form Fields ---
            SectionTitle(title = "Code Details")

            // ICD-10 Code
            OutlinedTextField(
                value = icdCode,
                onValueChange = viewModel::setIcdCode,
                label = { Text("ICD-10 Code*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                // Consider adding visual indication for error from viewModel if needed
                isError = errorMessage?.contains("Code", ignoreCase = true) == true
            )
            if (errorMessage?.contains("Code", ignoreCase = true) == true){
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }


            Spacer(modifier = Modifier.height(8.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description*") },
                modifier = Modifier.fillMaxWidth().height(100.dp), // Allow multi-line
                maxLines = 3,
                isError = errorMessage?.contains("Description", ignoreCase = true) == true
            )
            if (errorMessage?.contains("Description", ignoreCase = true) == true){
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Shorthand
            OutlinedTextField(
                value = shorthand,
                onValueChange = viewModel::setShorthand,
                label = { Text("Shorthand (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Flags/Options
            SectionTitle(title = "Options")

            // Billable Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setIsBillable(!isBillable) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isBillable,
                    onCheckedChange = { viewModel.setIsBillable(it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Billable Code")
            }

            // Common Code Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setIsCommon(!isCommon) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCommon,
                    onCheckedChange = { viewModel.setIsCommon(it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Commonly Used Code")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveCode() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving // Disable while saving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Code")
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Add space at the bottom

        } // End Column
    } // End Scaffold

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        ConfirmationDialog(
            title = "Discard Changes?",
            message = "You have unsaved changes. Are you sure you want to discard them and go back?",
            onConfirm = onNavigateUp, // Discard and navigate up
            onDismiss = { showCancelDialog = false } // Stay on the screen
        )
    }
}