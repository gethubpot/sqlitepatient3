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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MedicalServices
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

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.setSelectedFile(uri, context)
        }
    }

    // Show import result as snackbar
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

                // Changed from 2-button to 3-button segmented control
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    SegmentedButton(
                        selected = importType == ImportType.PATIENTS,
                        onClick = { viewModel.setImportType(ImportType.PATIENTS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
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
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
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
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
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
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // File selection section
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Format help info
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

                        when (importType) {
                            ImportType.PATIENTS -> {
                                Text("Patient CSV should have the following headers:")
                                Text(
                                    text = "firstName, lastName, dateOfBirth (MM/DD/YYYY), isMale (true/false), medicareNumber, facilityCode",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Example:")
                                Text(
                                    text = "John,Doe,01/15/1950,true,123456789A,GH001",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            ImportType.FACILITIES -> {
                                Text("Facility CSV should have the following headers:")
                                Text(
                                    text = "name, facilityCode, address1, city, state, zipCode, phoneNumber, isActive (true/false)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Example:")
                                Text(
                                    text = "General Hospital,GH001,123 Main St,Metropolis,NY,10001,555-123-4567,true",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            ImportType.DIAGNOSES -> {
                                Text("Diagnosis CSV should have the following headers:")
                                Text(
                                    text = "patientUPI, icdCode, description, priority, isHospiceCode (true/false), active (true/false)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Example:")
                                Text(
                                    text = "smijoh530915,I10,Essential (primary) hypertension,1,false,true",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Note: Patient UPI format is first 3 letters of lastName + first 3 letters of firstName + birth date in YYMMDD format",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import button
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
                    Text(if (isLoading) "Importing..." else "Import Data")
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Importing data...",
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
        ConfirmationDialog(
            title = "Confirm Import",
            message = when(importType) {
                ImportType.PATIENTS -> "Are you sure you want to import this patient data? This will add new patients to your database."
                ImportType.FACILITIES -> "Are you sure you want to import this facility data? This will add new facilities to your database."
                ImportType.DIAGNOSES -> "Are you sure you want to import this diagnosis data? This will add new diagnoses to your database. Make sure you have already imported the patients."
            },
            onConfirm = {
                scope.launch {
                    viewModel.importData(context)
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}