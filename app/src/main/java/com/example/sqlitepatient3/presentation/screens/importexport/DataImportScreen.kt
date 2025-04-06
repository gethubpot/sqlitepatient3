package com.example.sqlitepatient3.presentation.screens.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business // Facility icon
import androidx.compose.material.icons.filled.FileUpload // Upload icon
import androidx.compose.material.icons.filled.Info // Info icon
import androidx.compose.material.icons.filled.ListAlt // Alternative for Code Library
import androidx.compose.material.icons.filled.MedicalServices // Diagnosis icon
import androidx.compose.material.icons.filled.Person // Patient icon
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

                // --- MODIFIED Segmented Button Row ---
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp) // Use fillMaxWidth
                ) {
                    SegmentedButton(
                        selected = importType == ImportType.PATIENTS,
                        onClick = { viewModel.setImportType(ImportType.PATIENTS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4), // count = 4
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Patients")
                    }

                    SegmentedButton(
                        selected = importType == ImportType.FACILITIES,
                        onClick = { viewModel.setImportType(ImportType.FACILITIES) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4), // count = 4
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Facilities")
                    }

                    SegmentedButton(
                        selected = importType == ImportType.DIAGNOSES,
                        onClick = { viewModel.setImportType(ImportType.DIAGNOSES) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4), // count = 4
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Diagnoses")
                    }
                    // --- ADDED Code Library Button ---
                    SegmentedButton(
                        selected = importType == ImportType.CODE_LIBRARY,
                        onClick = { viewModel.setImportType(ImportType.CODE_LIBRARY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4), // index = 3, count = 4
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ListAlt, // Or Icons.Default.Code
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Code Library")
                    }
                    // --- END ADDED Code Library Button ---
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
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select CSV File")
                        }

                        if (fileName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Selected file: $fileName",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center // Center filename
                            )
                        }
                    }
                }

                // Format help info section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                        // --- MODIFIED Help Text ---
                        when (importType) {
                            ImportType.PATIENTS -> {
                                Text("Patient CSV must have headers:")
                                Text(
                                    text = "• Required: firstName, lastName", // Highlight required
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "• Optional: dateOfBirth (MM/DD/YYYY), isMale (true/false), medicareNumber, facilityCode, isHospice, onCcm, onPsych, onPsyMed",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Example:")
                                Text(
                                    text = "John,Doe,01/15/1950,true,123456789A,GH001,false,true,false,false",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            ImportType.FACILITIES -> {
                                Text("Facility CSV must have headers:")
                                Text(
                                    text = "• Required: name", // Highlight required
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "• Optional: facilityCode, address1, address2, city, state, zipCode, phoneNumber, faxNumber, email, isActive (true/false)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Example:")
                                Text(
                                    text = "General Hospital,GH001,123 Main St,,,Metropolis,NY,10001,555-1234,555-5678,,true",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            ImportType.DIAGNOSES -> {
                                Text("Diagnosis Link CSV must have headers:")
                                Text(
                                    text = "• Required: patientUPI, icdCode, description", // Highlight required
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "• Optional: priority (number, default 1), isHospiceCode (true/false), active (true/false)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Example:")
                                Text(
                                    text = "doejoh500115,I10,Essential Hypertension,1,false,true",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: Imports patient links. Only adds new codes to the library if they don't exist. Does NOT update existing code descriptions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error // Or onSurfaceVariant
                                )
                            }
                            // --- ADDED Code Library Help ---
                            ImportType.CODE_LIBRARY -> {
                                Text("Code Library CSV must have headers:")
                                Text(
                                    text = "• Required: icdCode (or code), description (or desc)", // Highlight required & alternatives
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "• Optional: shorthand, billable (true/false)", // Add optional if importer handles them
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Example:")
                                Text(
                                    text = "I10,Essential (primary) hypertension", // Simple example
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: Updates descriptions of existing codes. Can optionally add new codes if not found.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary // Or onSurfaceVariant
                                )
                            }
                            // --- END ADDED Code Library Help ---
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
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "Importing..." else "Start Import") // Changed text slightly
                }
            }

            // Loading overlay remains the same
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), // Consider matching scaffold padding
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp) // Add padding around indicator and text
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) // Optional overlay background
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
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        // --- MODIFIED Confirmation Dialog Message ---
        val confirmationMessage = when(importType) {
            ImportType.PATIENTS -> "Importing will add new patients from the CSV. Are you sure?"
            ImportType.FACILITIES -> "Importing will add new facilities from the CSV. Are you sure?"
            ImportType.DIAGNOSES -> "Importing will link patients to diagnoses from the CSV. It will only add new codes if they don't already exist in your library. Continue?"
            ImportType.CODE_LIBRARY -> "Importing will update descriptions for existing codes in your library based on the CSV. It may also add new codes if they are not found. Continue?"
        }
        ConfirmationDialog(
            title = "Confirm Import",
            message = confirmationMessage,
            onConfirm = {
                scope.launch {
                    viewModel.importData(context)
                }
                // Keep dialog open until import finishes? No, let Snackbar show result.
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false },
            confirmText = "Yes, Import" // Changed confirm text
        )
        // --- END MODIFIED Confirmation Dialog Message ---
    }
}