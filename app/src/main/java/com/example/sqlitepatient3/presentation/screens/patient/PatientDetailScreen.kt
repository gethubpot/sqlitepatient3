package com.example.sqlitepatient3.presentation.screens.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    onNavigateUp: () -> Unit,
    onEditPatient: (Long) -> Unit,
    onAddEvent: (Long) -> Unit,
    onViewAllEvents: (Long) -> Unit,
    onViewDiagnoses: (Long) -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val patient by viewModel.patient.collectAsState()
    val facilityName by viewModel.facilityName.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val diagnoses by viewModel.diagnoses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Handle loading state
    if (isLoading) {
        LoadingScaffold(
            title = "Patient Details",
            onNavigateUp = onNavigateUp
        )
        return
    }

    // Patient not found
    if (patient == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Patient Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "Patient not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Details") },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditPatient(patient?.id ?: 0) },
                icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                text = { Text("Edit Patient") }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Patient header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "${patient?.lastName}, ${patient?.firstName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "UPI: ${patient?.upi}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    patient?.dateOfBirth?.let {
                        Text(
                            text = "DOB: ${it.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))} (${patient?.isMale?.let { if (it) "Male" else "Female" } ?: ""})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!patient?.medicareNumber.isNullOrBlank()) {
                        Text(
                            text = "Medicare #: ${patient?.medicareNumber}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    facilityName?.let {
                        Text(
                            text = "Facility: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Care Programs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Care Programs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CareStatusTag(
                            title = "Hospice",
                            isActive = patient?.isHospice ?: false
                        )

                        CareStatusTag(
                            title = "CCM",
                            isActive = patient?.onCcm ?: false
                        )

                        CareStatusTag(
                            title = "Psych",
                            isActive = patient?.onPsych ?: false
                        )

                        CareStatusTag(
                            title = "Psych Med",
                            isActive = patient?.onPsyMed ?: false
                        )
                    }
                }
            }

            // Diagnoses Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diagnoses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { onViewDiagnoses(patient?.id ?: 0) }) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = "View All Diagnoses"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (diagnoses.isEmpty()) {
                        Text(
                            text = "No diagnoses recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        diagnoses.take(3).forEach { diagnosis ->
                            Text(
                                text = "• ${diagnosis.icdCode}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (diagnoses.size > 3) {
                            Text(
                                text = "... and ${diagnoses.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Recent Events
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Events",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            IconButton(onClick = { onAddEvent(patient?.id ?: 0) }) {
                                Icon(
                                    imageVector = Icons.Default.NoteAdd,
                                    contentDescription = "Add Event"
                                )
                            }

                            IconButton(onClick = { onViewAllEvents(patient?.id ?: 0) }) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = "View All Events"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (recentEvents.isEmpty()) {
                        Text(
                            text = "No events recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column {
                            recentEvents.forEach { event ->
                                Text(
                                    text = "• ${event.eventDateTime.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}: ${event.eventType}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CareStatusTag(
    title: String,
    isActive: Boolean
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    androidx.compose.material3.Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}