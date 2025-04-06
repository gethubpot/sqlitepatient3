package com.example.sqlitepatient3.presentation.screens.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions // ** ADDED import **
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// *** ADDED imports for new Icons ***
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
// *** END ADDED imports ***
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
import androidx.compose.ui.text.input.KeyboardType // ** ADDED import **
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
import kotlinx.coroutines.launch
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
    // *** MODIFIED: Use PatientSearchResultItem list ***
    val filteredPatients by viewModel.filteredPatients.collectAsState()
    // *** ADDED: Collect facility code for selected patient ***
    val selectedPatientFacilityCode by viewModel.selectedPatientFacilityCode.collectAsState()


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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show Snackbar and navigate when message appears
    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                viewModel.clearSaveMessage()
                onSaveComplete()
            }
        }
    }

    // Request focus for the patient search field when it's initially shown
    LaunchedEffect(selectedPatient) {
        if (selectedPatient == null && patientId == null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Handle loading state
    if (isLoading && eventId != null) {
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
                    // *** MODIFIED: Display selected patient with facility code ***
                    val displayText = buildString {
                        append("${selectedPatient!!.lastName}, ${selectedPatient!!.firstName}")
                        selectedPatientFacilityCode?.takeIf { it.isNotBlank() }?.let { code ->
                            append(" ($code)")
                        }
                    }
                    OutlinedTextField(
                        value = displayText,
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
                    // *** END MODIFIED ***
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

                    // *** MODIFIED: Use PatientSearchResultItem ***
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
                                    // Iterate over PatientSearchResultItem
                                    filteredPatients.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setPatientId(item.patient.id) // Set patient ID
                                                    viewModel.setPatientSearchQuery("") // Clear search
                                                    showPatientResults = false
                                                    keyboardController?.hide()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Display Name and Facility Code
                                            val patientText = buildString {
                                                append("${item.patient.lastName}, ${item.patient.firstName}")
                                                item.facilityCode?.takeIf { it.isNotBlank() }?.let { code ->
                                                    append(" ($code)")
                                                }
                                            }
                                            Text(
                                                text = patientText,
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
                    // *** END MODIFIED ***
                }
            }

            // --- Event Type, Date, F/U Row ---
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp), // Space between items
                verticalAlignment = Alignment.Top // Align items to top in case labels wrap
            ) {
                // Event Type Selection
                Box(modifier = Modifier.weight(1f)) { // Use weight
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
                            modifier = Modifier.fillMaxWidth().menuAnchor(), // Fill width within weight
                            singleLine = true // Try to keep label on one line
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

                // Date Selection
                OutlinedTextField(
                    value = eventDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                    onValueChange = { },
                    label = { Text("Date*") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Select Date")
                        }
                    },
                    modifier = Modifier.weight(1f), // Use weight
                    singleLine = true
                )

                // Follow-up Recurrence
                Box(modifier = Modifier.weight(1f)) { // Use weight
                    ExposedDropdownMenuBox(
                        expanded = followUpRecurrenceDropdownExpanded,
                        onExpandedChange = { followUpRecurrenceDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = followUpRecurrence.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("F/U") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = followUpRecurrenceDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), // Fill width within weight
                            singleLine = true // Try to keep label on one line
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
            }
            // --- END Event Type, Date, F/U Row ---


            Spacer(modifier = Modifier.height(8.dp)) // Keep one spacer for separation

            // Visit Type & Location (Conditional)
            if (eventType == EventType.FACE_TO_FACE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Visit Type
                    Box(modifier = Modifier.weight(1f)) {
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
                    // Visit Location
                    Box(modifier = Modifier.weight(1f)) {
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


            // --- Event Duration Section ---
            Text( // Optional Label above the Row
                text = "Duration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly // Keep this arrangement
            ) {
                // Decrement Button
                IconButton(onClick = { viewModel.decrementDurationRandomly() }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Duration")
                }

                // Event Duration Text Field
                OutlinedTextField(
                    value = eventMinutes.toString(),
                    onValueChange = {
                        // Allow direct editing, ensure it's a valid number >= 1
                        val newValue = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 1
                        viewModel.setEventMinutes(newValue)
                    },
                    label = { Text("Min") },
                    keyboardOptions = KeyboardOptions( // Use imported KeyboardOptions
                        keyboardType = KeyboardType.Number // Use imported KeyboardType
                    ),
                    modifier = Modifier.width(80.dp), // Keep adjusted width
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center) // Center text inside
                )

                // Increment Button
                IconButton(onClick = { viewModel.incrementDurationRandomly() }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Duration")
                }
            }
            // --- END Event Duration Section ---

            Spacer(modifier = Modifier.height(8.dp))


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
} // End AddEditEventScreen composable