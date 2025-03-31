package com.example.sqlitepatient3.presentation.screens.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.presentation.components.ConfirmationDialog
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import com.example.sqlitepatient3.presentation.components.SectionTitle // Keep if used for Notes title
// Removed import for FormField as we are creating Rows directly
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onNavigateUp: () -> Unit,
    onEditEvent: (Long) -> Unit,
    onPatientClick: (Long) -> Unit, // Navigate to patient detail
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val event by viewModel.event.collectAsState()
    val patient by viewModel.patient.collectAsState()
    val facility by viewModel.facility.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Formatters
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd/yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' HH:mm") }

    // --- Loading State ---
    if (isLoading) {
        LoadingScaffold(title = "Event Details", onNavigateUp = onNavigateUp)
        return
    }

    // --- Error or Event Not Found State ---
    if (event == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Event Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "Event could not be loaded.",
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
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditEvent(event!!.id) }) {
                        Icon(Icons.Default.Edit, "Edit Event")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete Event")
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
            // Display Error if any (non-fatal error, like patient/facility loading)
            errorMessage?.let { error ->
                if (event != null) { // Only show if event loaded but other things failed
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Patient Information Card
            patient?.let { pat ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    onClick = { onPatientClick(pat.id) } // Make card clickable
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${pat.lastName}, ${pat.firstName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        // Removed UPI line:
                        // Text("UPI: ${pat.upi}", style = MaterialTheme.typography.bodyMedium)
                        facility?.let { fac ->
                            Text("Facility: ${fac.getDisplayName()}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } ?: run {
                // Show placeholder if patient data couldn't be loaded
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text("Patient details unavailable", modifier = Modifier.padding(16.dp))
                }
            }

            // *** MOVED Notes Card ***
            if (!event!!.noteText.isNullOrBlank()) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom=16.dp)) { // Added padding here
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Notes") // Keep or remove title as desired
                        Text(
                            text = event!!.noteText!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Event Details Card
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Removed SectionTitle("Event Information")

                    // *** Detail Rows section remains the same ***
                    DetailRow(label = "Event Type:", value = event!!.eventType.toString())
                    DetailRow(label = "Date & Time:", value = event!!.eventDateTime.format(dateTimeFormatter))
                    DetailRow(label = "Duration:", value = "${event!!.eventMinutes} minutes")
                    DetailRow(label = "Status:", value = event!!.status.toString())

                    if (event!!.eventType == com.example.sqlitepatient3.domain.model.EventType.FACE_TO_FACE) {
                        DetailRow(label = "Visit Type:", value = event!!.visitType.toString())
                        DetailRow(label = "Visit Location:", value = event!!.visitLocation.toString())
                    }

                    if (event!!.eventType == com.example.sqlitepatient3.domain.model.EventType.TCM) {
                        DetailRow(label = "Hosp Discharge:", value = event!!.hospDischargeDate?.format(dateFormatter) ?: "N/A")
                    }

                    DetailRow(label = "Follow-up:", value = event!!.followUpRecurrence.toString())
                    event!!.calculateNextFollowUpDate()?.let { nextDate ->
                        DetailRow(label = "Next Follow-up Due:", value = nextDate.format(dateFormatter))
                    }

                    DetailRow(label = "Bill Date:", value = event!!.eventBillDate.format(dateFormatter))
                    // *** End Detail Rows section ***

                    // CPT Code and Modifier (if applicable) - Using Rows as well
                    if (!event!!.cptCode.isNullOrBlank() || !event!!.modifier.isNullOrBlank()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Billing Codes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (!event!!.cptCode.isNullOrBlank()) {
                            DetailRow(label = "CPT Code:", value = event!!.cptCode!!)
                        }
                        if (!event!!.modifier.isNullOrBlank()) {
                            DetailRow(label = "Modifier:", value = event!!.modifier!!)
                        }
                    }
                }
            }

            // *** Notes Card was MOVED above ***

        } // End Column
    } // End Scaffold

    // --- Confirmation Dialogs ---
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Event?",
            message = "Are you sure you want to permanently delete this event?",
            confirmText = "Delete",
            onConfirm = {
                // TODO: Implement delete functionality in ViewModel
                // viewModel.deleteEvent()
                onNavigateUp() // Navigate up after deletion (or confirmation)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

/**
 * Simple Composable to display a label and value on the same row.
 */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Add vertical padding for spacing between rows
        verticalAlignment = Alignment.Top // Align items to the top in case value wraps
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp) // Adjust label width as needed
        )
        Spacer(modifier = Modifier.width(8.dp)) // Space between label and value
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f) // Allow value to take remaining space
        )
    }
}