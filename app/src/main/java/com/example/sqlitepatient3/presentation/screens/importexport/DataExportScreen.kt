package com.example.sqlitepatient3.presentation.screens.importexport

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material.icons.filled.Event

enum class ExportType {
    PATIENTS, FACILITIES, EVENTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(
    onNavigateUp: () -> Unit,
    viewModel: DataExportViewModel = hiltViewModel()
) {
    val exportType by viewModel.exportType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showShareDialog by remember { mutableStateOf(false) }

    // File picker for saving file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.exportData(context, uri)
            }
        }
    }

    // Show export result as snackbar
    LaunchedEffect(exportResult) {
        exportResult?.let { result ->
            snackbarHostState.showSnackbar(result.message)

            // If export was successful and we have a file, show share dialog
            if (result.success && result.fileUri != null) {
                showShareDialog = true
            }

            viewModel.clearExportResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data") },
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
                // Export type selector
                Text(
                    text = "What would you like to export?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    SegmentedButton(
                        selected = exportType == ExportType.PATIENTS,
                        onClick = { viewModel.setExportType(ExportType.PATIENTS) },
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
                        selected = exportType == ExportType.FACILITIES,
                        onClick = { viewModel.setExportType(ExportType.FACILITIES) },
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
                        selected = exportType == ExportType.EVENTS,
                        onClick = { viewModel.setExportType(ExportType.EVENTS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Event, // This line caused the error
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text("Events")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Export options section
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
                            text = "Export Options",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val fileName = if (exportType == ExportType.PATIENTS) "patients.csv" else "facilities.csv"
                                saveFileLauncher.launch(fileName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to File")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.exportData(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Export")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Quick export saves to app's documents folder.\nYou can then share the file.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Format info section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Export Format",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (exportType == ExportType.PATIENTS) {
                            Text("Patient CSV will include:")
                            Text(
                                text = "• firstName, lastName, dateOfBirth, isMale\n• medicareNumber, facilityCode\n• isHospice, onCcm, onPsych, onPsyMed",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (exportType == ExportType.FACILITIES) {
                            Text("Facility CSV will include:")
                            Text(
                                text = "• name, facilityCode, address1, address2\n• city, state, zipCode\n• phoneNumber, faxNumber, email\n• isActive",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (exportType == ExportType.EVENTS) {
                            Text("Events CSV will include:")
                            Text(
                                text = "• patientUpi, eventDateTime, eventBillDate\n• eventMinutes, ccmMinutes, noteText\n• eventType, status, diagnoses\n• ttddDate, hospDisDate, eventFile",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
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
                            text = "Exporting data...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Handle file sharing
    if (showShareDialog && exportResult?.fileUri != null) {
        LaunchedEffect(Unit) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, exportResult?.fileUri)
                type = "text/csv"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share CSV File"))
            showShareDialog = false
        }
    }
}