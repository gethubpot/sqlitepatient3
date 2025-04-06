package com.example.sqlitepatient3.presentation.screens.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.presentation.components.ConfirmationDialog
import kotlinx.coroutines.launch

// Import the updated Enum if it's defined in the ViewModel file
// import com.example.sqlitepatient3.presentation.screens.importexport.ImportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataImportScreen(
    onNavigateUp: () -> Unit,
    viewModel: DataImportViewModel = hiltViewModel()
) {
    val importType by viewModel.importType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val fileName by viewModel.fileName.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showConfirmDialog by remember { mutableStateOf(false) }

    // File picker remains the same
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.setSelectedFile(it, context)
        }
    }

    // Show import result as snackbar remains the same
    LaunchedEffect(importResult) {
        importResult?.let { result ->
            snackbarHostState.showSnackbar(result)
            viewModel.clearImportResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Import type selector
                Text(
                    text = "What would you like to import?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // --- MODIFIED Segmented Button Row with NEW LABELS ---
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    SegmentedButton(
                        selected = importType == ImportType.PATIENTS,
                        onClick = { viewModel.setImportType(ImportType.PATIENTS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Pts") // <<<--- CHANGED LABEL
                    }

                    SegmentedButton(
                        selected = importType == ImportType.FACILITIES,
                        onClick = { viewModel.setImportType(ImportType.FACILITIES) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Fac") // <<<--- CHANGED LABEL
                    }

                    SegmentedButton(
                        selected = importType == ImportType.DIAGNOSES,
                        onClick = { viewModel.setImportType(ImportType.DIAGNOSES) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Dx-Pt") // <<<--- CHANGED LABEL
                    }

                    SegmentedButton(
                        selected = importType == ImportType.CODE_LIBRARY,
                        onClick = { viewModel.setImportType(ImportType.CODE_LIBRARY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ListAlt, // Or Icons.Default.Code
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("ICD-10") // <<<--- CHANGED LABEL
                    }
                }
                // --- END MODIFIED Segmented Button Row ---


                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // File selection section remains the same
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Select CSV File to Import",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { filePickerLauncher.launch("text/csv") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select CSV File")
                        }
                        if (fileName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Selected file: $fileName",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Format help info section remains the same
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Required CSV Format",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Help text content remains the same (adjust if needed based on new short names)
                        when (importType) {
                            ImportType.PATIENTS -> { /* ... help text ... */ }
                            ImportType.FACILITIES -> { /* ... help text ... */ }
                            ImportType.DIAGNOSES -> { /* ... help text ... */ }
                            ImportType.CODE_LIBRARY -> { /* ... help text ... */ }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import button remains the same
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = fileName.isNotBlank() && !isLoading
                ) {
                    if (isLoading) { /* ... loading indicator ... */ }
                    Text(if (isLoading) "Importing..." else "Start Import")
                }
            }

            // Loading overlay remains the same
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // Use scaffold padding
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Importing data...\nPlease wait.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } // End Box within Scaffold
    } // End Scaffold

    // Confirmation dialog remains the same
    if (showConfirmDialog) {
        val confirmationMessage = when(importType) {
            ImportType.PATIENTS -> "Importing will add new patients (Pts) from the CSV. Are you sure?"
            ImportType.FACILITIES -> "Importing will add new facilities (Fac) from the CSV. Are you sure?"
            ImportType.DIAGNOSES -> "Importing will link patients to diagnoses (Dx-Pt) from the CSV. It will only add new codes if they don't already exist in your library. Continue?"
            ImportType.CODE_LIBRARY -> "Importing will update descriptions for existing ICD-10 codes in your library based on the CSV. It may also add new codes if they are not found. Continue?"
        }
        ConfirmationDialog(
            title = "Confirm Import",
            message = confirmationMessage,
            onConfirm = {
                scope.launch { viewModel.importData(context) }
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false },
            confirmText = "Yes, Import"
        )
    }
} // End DataImportScreen