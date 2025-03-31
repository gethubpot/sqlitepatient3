package com.example.sqlitepatient3.presentation.screens.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch // ** ADDED import **
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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

    // Observe the new saveMessage state
    val saveMessage by viewModel.saveMessage.collectAsState()

    val selectedPatient by viewModel.selectedPatient.collectAsState()
    val eventType by viewModel.eventType.collectAsState()
    val visitType by viewModel.visitType.collectAsState()
    val visitLocation by viewModel.visitLocation.collectAsState()
    val eventDateTime by viewModel.eventDateTime.collectAsState()
    val eventMinutes by viewModel.eventMinutes.collectAsState()
    val noteText by viewModel.noteText.collectAsState()
    val followUpRecurrence by viewModel.followUpRecurrence.collectAsState()
    val hospDischargeDate by viewModel.hospDischargeDate.collectAsState()
    val patientSearchQuery by viewModel.patientSearchQuery.collectAsState()
    val filteredPatients by viewModel.filteredPatients.collectAsState()

    // State variables for UI components
    var showDatePicker by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var eventTypesDropdownExpanded by remember { mutableStateOf(false) }
    var visitTypeDropdownExpanded by remember { mutableStateOf(false) }
    var visitLocationDropdownExpanded by remember { mutableStateOf(false) }
    var followUpRecurrenceDropdownExpanded by remember { mutableStateOf(false) }
    var showPatientResults by remember { mutableStateOf(false) }

    // Focus Requester and Keyboard Controller
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ** ADDED SnackbarHostState and CoroutineScope **
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show Snackbar and navigate when message appears
    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                // Clear the message in the ViewModel *after* showing Snackbar
                viewModel.clearSaveMessage()
                // Call navigation callback *after* Snackbar logic
                onSaveComplete()
            }
        }
    }

    // REMOVED the LaunchedEffect that observed saveSuccess
    // LaunchedEffect(saveSuccess) {
    //     if (saveSuccess) {
    //         onSaveComplete()
    //     }
    // }

    // Request focus for the patient search field when it's initially shown
    LaunchedEffect(selectedPatient) {
        if (selectedPatient == null && patientId == null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }


    // Handle loading state
    if (isLoading && eventId != null) { // Adjusted loading check
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
        },
        // ** ADDED SnackbarHost to the Scaffold **
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                if (selectedPatient != null) {
                    OutlinedTextField(
                        value = "${selectedPatient!!.lastName}, ${selectedPatient!!.firstName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected Patient*") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.setPatientId(null) }) {
                                Icon(Icons.Default.Clear, "Clear Patient")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = patientSearchQuery,
                        onValueChange = {
                            viewModel.setPatientSearchQuery(it)
                            showPatientResults = it.isNotBlank()
                        },
                        label = { Text("Search Patient*") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        trailingIcon = {
                            if (patientSearchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setPatientSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, "Clear Search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true
                    )

                    if (showPatientResults && patientSearchQuery.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (filteredPatients.isEmpty()) {
                                    Text(
                                        text = "No matching patients found",
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        textAlign = TextAlign.Center,
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
                                                    keyboardController?.hide()
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
            Box(modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier.fillMaxWidth().menuAnchor()
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

            // Visit Type & Location (Conditional)
            if (eventType == EventType.FACE_TO_FACE) {
                // Visit Type
                Box(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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

                // Visit Location
                Box(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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

            // Hospital Discharge Date (Conditional)
            if (eventType == EventType.TCM) {
                OutlinedTextField(
                    value = hospDischargeDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "Not set",
                    onValueChange = { },
                    label = { Text("Hospital Discharge Date*") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.setHospDischargeDate(null) }) {
                            Icon(Icons.Default.Clear, "Clear Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = if (hospDischargeDate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        focusedBorderColor = if (hospDischargeDate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                )
                if (hospDischargeDate == null) {
                    Text(
                        text = "Hospital discharge date is required for TCM events",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Date Selection
            OutlinedTextField(
                value = eventDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                onValueChange = { },
                label = { Text("Date*") },
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "Select Date")
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
            Box(modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier.fillMaxWidth().menuAnchor()
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
                // Enable button only if not saving AND a patient is selected
                enabled = !isSaving && selectedPatient != null
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
        } // End Column
    } // End Scaffold padding

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
} // End AddEditEventScreen composableas