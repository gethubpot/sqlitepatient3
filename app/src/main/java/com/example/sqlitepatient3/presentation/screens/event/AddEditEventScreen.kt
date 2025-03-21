package com.example.sqlitepatient3.presentation.screens.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.domain.model.EventType
import com.example.sqlitepatient3.domain.model.FollowUpRecurrence
import com.example.sqlitepatient3.domain.model.VisitLocation
import com.example.sqlitepatient3.domain.model.VisitType
import com.example.sqlitepatient3.presentation.components.ConfirmationDialog
import com.example.sqlitepatient3.presentation.components.DatePickerDialog
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import com.example.sqlitepatient3.presentation.components.SectionTitle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    eventId: Long? = null,
    patientId: Long? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AddEditEventViewModel = hiltViewModel()
) {
    // Initialize the view model with patient ID if provided
    LaunchedEffect(patientId) {
        patientId?.let { viewModel.setPatientId(it) }
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    val selectedPatient by viewModel.selectedPatient.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val eventType by viewModel.eventType.collectAsState()
    val visitType by viewModel.visitType.collectAsState()
    val visitLocation by viewModel.visitLocation.collectAsState()
    val eventDateTime by viewModel.eventDateTime.collectAsState()
    val eventMinutes by viewModel.eventMinutes.collectAsState()
    val noteText by viewModel.noteText.collectAsState()
    val followUpRecurrence by viewModel.followUpRecurrence.collectAsState()

    // Add state for patient search
    val patientSearchQuery by viewModel.patientSearchQuery.collectAsState()
    val filteredPatients by viewModel.filteredPatients.collectAsState()

    // State variables for UI components
    var showDatePicker by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var eventTypesDropdownExpanded by remember { mutableStateOf(false) }
    var visitTypeDropdownExpanded by remember { mutableStateOf(false) }
    var visitLocationDropdownExpanded by remember { mutableStateOf(false) }
    var followUpRecurrenceDropdownExpanded by remember { mutableStateOf(false) }

    // Replace the dropdown expanded state with search results expanded state
    var showPatientResults by remember { mutableStateOf(false) }

    // If save was successful, navigate back
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onSaveComplete()
        }
    }

    // Handle loading state
    if (isLoading) {
        LoadingScaffold(
            title = if (eventId == null) "Schedule Event" else "Edit Event",
            onNavigateUp = onNavigateUp
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == null) "Schedule Event" else "Edit Event") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Check if there are unsaved changes before navigating up
                        if (selectedPatient != null || noteText.isNotBlank()) {
                            showCancelDialog = true
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .verticalScroll(rememberScrollState())
        ) {
            // Error message
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
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = { viewModel.clearError() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Basic Information Section
            SectionTitle(title = "Basic Information")

            // Patient Selection with Search-as-you-type
            Column(modifier = Modifier.fillMaxWidth()) {
                // If a patient is already selected, show their name with an option to clear
                if (selectedPatient != null) {
                    OutlinedTextField(
                        value = "${selectedPatient!!.lastName}, ${selectedPatient!!.firstName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected Patient*") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.setPatientId(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Patient"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    // Search field
                    OutlinedTextField(
                        value = patientSearchQuery,
                        onValueChange = {
                            viewModel.setPatientSearchQuery(it)
                            showPatientResults = it.isNotBlank()
                        },
                        label = { Text("Search Patient*") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (patientSearchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setPatientSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Search results
                    if (showPatientResults && patientSearchQuery.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (filteredPatients.isEmpty()) {
                                    Text(
                                        text = "No matching patients found",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    filteredPatients.forEach { patient ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setPatientId(patient.id)
                                                    viewModel.setPatientSearchQuery("")
                                                    showPatientResults = false
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${patient.lastName}, ${patient.firstName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event Type Selection
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = eventTypesDropdownExpanded,
                    onExpandedChange = { eventTypesDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = eventType.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventTypesDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = eventTypesDropdownExpanded,
                        onDismissRequest = { eventTypesDropdownExpanded = false }
                    ) {
                        EventType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.toString()) },
                                onClick = {
                                    viewModel.setEventType(type)
                                    eventTypesDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visit Type Selection (only shown for certain event types)
            if (eventType == EventType.FACE_TO_FACE) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = visitTypeDropdownExpanded,
                        onExpandedChange = { visitTypeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = visitType.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Visit Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = visitTypeDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = visitTypeDropdownExpanded,
                            onDismissRequest = { visitTypeDropdownExpanded = false }
                        ) {
                            VisitType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.toString()) },
                                    onClick = {
                                        viewModel.setVisitType(type)
                                        visitTypeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Visit Location Selection (only shown for face-to-face events)
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = visitLocationDropdownExpanded,
                        onExpandedChange = { visitLocationDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = visitLocation.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Visit Location") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = visitLocationDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = visitLocationDropdownExpanded,
                            onDismissRequest = { visitLocationDropdownExpanded = false }
                        ) {
                            VisitLocation.values().forEach { location ->
                                DropdownMenuItem(
                                    text = { Text(location.toString()) },
                                    onClick = {
                                        viewModel.setVisitLocation(location)
                                        visitLocationDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Date Selection
            OutlinedTextField(
                value = eventDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                onValueChange = { },
                label = { Text("Date*") },
                readOnly = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))


            // Event Duration
            OutlinedTextField(
                value = eventMinutes.toString(),
                onValueChange = {
                    val newValue = it.toIntOrNull() ?: 0
                    viewModel.setEventMinutes(newValue)
                },
                label = { Text("Duration (minutes)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Follow-up Recurrence
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = followUpRecurrenceDropdownExpanded,
                    onExpandedChange = { followUpRecurrenceDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = followUpRecurrence.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Follow-up Recurrence") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = followUpRecurrenceDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = followUpRecurrenceDropdownExpanded,
                        onDismissRequest = { followUpRecurrenceDropdownExpanded = false }
                    ) {
                        FollowUpRecurrence.values().forEach { recurrence ->
                            DropdownMenuItem(
                                text = { Text(recurrence.toString()) },
                                onClick = {
                                    viewModel.setFollowUpRecurrence(recurrence)
                                    followUpRecurrenceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes Section
            SectionTitle(title = "Notes")

            OutlinedTextField(
                value = noteText,
                onValueChange = { viewModel.setNoteText(it) },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = { viewModel.saveEvent() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Event")
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val currentDate = eventDateTime.toLocalDate()
        val epochMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        DatePickerDialog(
            onDateSelected = { millis ->
                val selectedDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                viewModel.setEventDate(selectedDate)
            },
            onDismiss = { showDatePicker = false },
            initialSelectedDateMillis = epochMillis
        )
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        ConfirmationDialog(
            title = "Discard Changes?",
            message = "You have unsaved changes. Are you sure you want to discard them?",
            onConfirm = onNavigateUp,
            onDismiss = { showCancelDialog = false }
        )
    }
}