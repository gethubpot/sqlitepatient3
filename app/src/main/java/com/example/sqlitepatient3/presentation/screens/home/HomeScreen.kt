package com.example.sqlitepatient3.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Keep existing icons
import androidx.compose.material.icons.outlined.ListAlt // Example new icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector // Import needed for ActionButton icon param
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.ui.theme.SQLitePatient3Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPatientList: () -> Unit,
    onNavigateToFacilityList: () -> Unit,
    onNavigateToEventList: () -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToAddEvent: () -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDatabaseInfo: () -> Unit,
    onNavigateToDiagnosticCodes: () -> Unit, // <<<--- ADDED PARAMETER
    viewModel: HomeViewModel = hiltViewModel()
) {
    val patientCount by viewModel.patientCount.collectAsState()
    val eventCount by viewModel.eventCount.collectAsState()
    val facilityCount by viewModel.facilityCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Healthcare Management") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Cards (No changes here)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Patients",
                    count = patientCount,
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPatientList
                )
                StatCard(
                    title = "Events",
                    count = eventCount,
                    icon = Icons.Default.Event,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToEventList
                )
                StatCard(
                    title = "Facilities",
                    count = facilityCount,
                    icon = Icons.Default.Business,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToFacilityList
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Actions (No changes here)
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            ActionButton(
                text = "Add New Patient",
                icon = Icons.Default.PersonAdd,
                onClick = onNavigateToAddPatient
            )

            ActionButton(
                text = "Schedule Event",
                icon = Icons.Default.AddTask,
                onClick = onNavigateToAddEvent
            )

            ActionButton(
                text = "Import/Export Data",
                icon = Icons.Default.SwapHoriz,
                onClick = onNavigateToImportExport
            )

            // Database Management Section
            Text(
                text = "Data Management", // Changed title slightly
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            ActionButton( // <<<--- ADDED BUTTON
                text = "Manage Diagnostic Codes",
                icon = Icons.Outlined.ListAlt, // Or Icons.Default.MedicalServices
                onClick = onNavigateToDiagnosticCodes // Use the new lambda
            )

            ActionButton(
                text = "Database Information",
                icon = Icons.Default.Storage,
                onClick = onNavigateToDatabaseInfo
            )

            // Backup & Restore button (No changes here)
            // Note: This button currently navigates to DatabaseInfo first.
            // Consider changing its onClick to navigate directly to Backup/Restore if desired.
            ActionButton(
                text = "Backup & Restore",
                icon = Icons.Default.Backup,
                onClick = onNavigateToDatabaseInfo // Keep as is or change if needed
            )


            Spacer(modifier = Modifier.weight(1f))

            // App Version (No changes here)
            Text(
                text = "SQLitePatient3 v1.0", // Consider making this dynamic later
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// StatCard (No changes needed)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ActionButton (No changes needed)
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = text)
        }
    }
}

// Preview (Update to include new button)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SQLitePatient3Theme {
        // Create a preview-compatible HomeScreen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Cards (Preview)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = "Patients", count = 42, icon = Icons.Default.Person, modifier = Modifier.weight(1f), onClick = {})
                StatCard(title = "Events", count = 128, icon = Icons.Default.Event, modifier = Modifier.weight(1f), onClick = {})
                StatCard(title = "Facilities", count = 7, icon = Icons.Default.Business, modifier = Modifier.weight(1f), onClick = {})
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Actions (Preview)
            Text(text = "Quick Actions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            ActionButton(text = "Add New Patient", icon = Icons.Default.PersonAdd, onClick = {})
            ActionButton(text = "Schedule Event", icon = Icons.Default.AddTask, onClick = {})
            ActionButton(text = "Import/Export Data", icon = Icons.Default.SwapHoriz, onClick = {})

            // Data Management (Preview)
            Text(text = "Data Management", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            ActionButton(text = "Manage Diagnostic Codes", icon = Icons.Outlined.ListAlt, onClick = {}) // <<<--- ADDED BUTTON TO PREVIEW
            ActionButton(text = "Database Information", icon = Icons.Default.Storage, onClick = {})
            ActionButton(text = "Backup & Restore", icon = Icons.Default.Backup, onClick = {})
        }
    }
}