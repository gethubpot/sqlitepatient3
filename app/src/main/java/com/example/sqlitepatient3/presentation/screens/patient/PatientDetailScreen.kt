package com.example.sqlitepatient3.presentation.screens.patient

// Ensure all necessary imports are present
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val facilityCode by viewModel.facilityCode.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val diagnosesWithDescriptions by viewModel.diagnosesWithDescriptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // --- Loading State ---
    if (isLoading) {
        LoadingScaffold(
            title = "Patient Details",
            onNavigateUp = onNavigateUp
        )
        return
    }

    // --- Patient Not Found State ---
    if (patient == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Patient Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Apply padding
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

    // --- Main Content ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            patient?.id?.let { patientId ->
                ExtendedFloatingActionButton(
                    onClick = { onEditPatient(patientId) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                    text = { Text("Edit Patient") }
                )
            }
        }
    ) { paddingValues ->
        // Use safe call for patient data access within the content
        patient?.let { pat ->
            // *** MAIN SCROLLABLE COLUMN ***
            Column(
                modifier = Modifier
                    .fillMaxSize()              // Fill available space
                    .padding(paddingValues)     // Apply scaffold padding
                    .verticalScroll(rememberScrollState()) // Enable scrolling
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Content padding
            ) {
                // Error message display (if any)
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // *** Patient Header Card ***
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp) // Space below this card
                ) {
                    Column(modifier = Modifier.padding(16.dp)) { // Padding inside card
                        // Patient Name
                        Text(
                            text = "${pat.lastName}, ${pat.firstName}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Row for DOB, Facility Code, and Care Program Tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Spacing between items in the row
                        ) {
                            // DOB (no label)
                            pat.dateOfBirth?.let { dob ->
                                Text(
                                    text = dob.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Facility Code (no parentheses)
                            facilityCode?.takeIf { it.isNotBlank() }?.let { facCode ->
                                Text(
                                    text = facCode,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Spacer to push tags to the right if desired, otherwise remove or adjust weight
                            // Spacer(Modifier.weight(1f))

                            // Care Program Tags
                            CareStatusTag(title = "H", isActive = pat.isHospice)
                            CareStatusTag(title = "C", isActive = pat.onCcm)
                            CareStatusTag(title = "P", isActive = pat.onPsych)
                            CareStatusTag(title = "M", isActive = pat.onPsyMed)
                        }
                    }
                }
                // *** END Patient Header Card ***

                // *** Diagnoses Card ***
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp) // Space below this card
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp) // Padding inside card
                    ) {
                        if (diagnosesWithDescriptions.isEmpty()) {
                            Text(
                                text = "No active diagnoses recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val chunkedDiagnoses = diagnosesWithDescriptions.chunked(2)
                            chunkedDiagnoses.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        DiagnosisItem(item = pair[0])
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (pair.size > 1) {
                                            DiagnosisItem(item = pair[1])
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // *** END Diagnoses Card ***

                // *** Recent Events Card ***
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp) // Space below this card
                ) {
                    Column(modifier = Modifier.padding(16.dp)) { // Padding inside card
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
                            Row { // Group buttons
                                IconButton(onClick = { onAddEvent(pat.id) }) {
                                    Icon(Icons.Default.NoteAdd, "Add Event")
                                }
                                IconButton(onClick = { onViewAllEvents(pat.id) }) {
                                    Icon(Icons.Default.Event, "View All Events")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (recentEvents.isEmpty()) {
                            Text(
                                text = "No recent events found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            recentEvents.forEach { event ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text( /* Date */
                                        text = event.eventDateTime.format(DateTimeFormatter.ofPattern("MM/dd/yy")),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text( /* Type */
                                        text = event.eventType.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text( /* Minutes */
                                        text = "${event.eventMinutes} min",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                // *** END Recent Events Card ***

                // Spacer for FAB visibility when scrolled to bottom
                Spacer(modifier = Modifier.height(80.dp))

            } // End Main Scrollable Column
        } // End safe call block for patient
    } // End Scaffold
} // End PatientDetailScreen

// Helper composable for displaying a single diagnosis item
@Composable
fun DiagnosisItem(item: PatientDiagnosisWithDescription) {
    Text(
        text = "• ${item.diagnosis.icdCode} - ${item.description?.takeIf { it.isNotBlank() } ?: "..."}",
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = if (item.description.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current
    )
}

// CareStatusTag composable
@Composable
fun CareStatusTag(title: String, isActive: Boolean) {
    if (isActive) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}