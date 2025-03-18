package com.example.sqlitepatient3.presentation.screens.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.presentation.components.ConfirmationDialog
import com.example.sqlitepatient3.presentation.components.DatePickerDialog
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import com.example.sqlitepatient3.presentation.components.SectionTitle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPatientScreen(
    patientId: Long? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AddEditPatientViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val dateOfBirth by viewModel.dateOfBirth.collectAsState()
    val isMale by viewModel.isMale.collectAsState()
    val medicareNumber by viewModel.medicareNumber.collectAsState()
    val facilityId by viewModel.facilityId.collectAsState()
    val isHospice by viewModel.isHospice.collectAsState()
    val onCcm by viewModel.onCcm.collectAsState()
    val onPsych by viewModel.onPsych.collectAsState()
    val onPsyMed by viewModel.onPsyMed.collectAsState()

    val facilities by viewModel.facilities.collectAsState()

    // State variables for UI components
    var showDatePicker by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) } // For facility dropdown

    // If save was successful, navigate back
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onSaveComplete()
        }
    }

    // Handle loading state
    if (isLoading) {
        LoadingScaffold(
            title = if (patientId == null) "Add Patient" else "Edit Patient",
            onNavigateUp = onNavigateUp
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (patientId == null) "Add Patient" else "Edit Patient") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Check if there are unsaved changes before navigating up
                        if (firstName.isNotEmpty() || lastName.isNotEmpty() || dateOfBirth != null) {
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

            // Last Name
            OutlinedTextField(
                value = lastName,
                onValueChange = { viewModel.setLastName(it) },
                label = { Text("Last Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { viewModel.setFirstName(it) },
                label = { Text("First Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date of Birth
            OutlinedTextField(
                value = dateOfBirth?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "",
                onValueChange = { },
                label = { Text("Date of Birth") },
                readOnly = true,
                trailingIcon = {
                    Row {
                        if (dateOfBirth != null) {
                            IconButton(onClick = { viewModel.setDateOfBirth(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Date"
                                )
                            }
                        }

                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Date"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gender
            Column {
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isMale,
                        onClick = { viewModel.setIsMale(true) }
                    )
                    Text(
                        text = "Male",
                        modifier = Modifier.clickable { viewModel.setIsMale(true) }
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    RadioButton(
                        selected = !isMale,
                        onClick = { viewModel.setIsMale(false) }
                    )
                    Text(
                        text = "Female",
                        modifier = Modifier.clickable { viewModel.setIsMale(false) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Medicare Number
            OutlinedTextField(
                value = medicareNumber,
                onValueChange = { viewModel.setMedicareNumber(it) },
                label = { Text("Medicare Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Facility
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = facilities.find { it.id == facilityId }?.getDisplayName() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Facility") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Add "None" option
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                viewModel.setFacilityId(null)
                                expanded = false
                            }
                        )

                        // Add all facilities
                        facilities.forEach { facility ->
                            DropdownMenuItem(
                                text = { Text(facility.getDisplayName()) },
                                onClick = {
                                    viewModel.setFacilityId(facility.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Care Program Status
            SectionTitle(title = "Care Programs")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isHospice,
                    onCheckedChange = { viewModel.setIsHospice(it) }
                )
                Text("Hospice Care")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = onCcm,
                    onCheckedChange = { viewModel.setOnCcm(it) }
                )
                Text("Chronic Care Management (CCM)")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = onPsych,
                    onCheckedChange = { viewModel.setOnPsych(it) }
                )
                Text("Psychiatric Care")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = onPsyMed,
                    onCheckedChange = { viewModel.setOnPsyMed(it) }
                )
                Text("Psychiatric Medication")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = { viewModel.savePatient() },
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
                    Text("Save Patient")
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val epochMillis = dateOfBirth?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

        DatePickerDialog(
            onDateSelected = { millis ->
                val selectedDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                viewModel.setDateOfBirth(selectedDate)
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