package com.example.sqlitepatient3.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            // Stats Cards
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

            // Action Buttons
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
                text = "Database Management",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            ActionButton(
                text = "Database Information",
                icon = Icons.Default.Storage,
                onClick = onNavigateToDatabaseInfo
            )

            // New Backup & Restore button
            ActionButton(
                text = "Backup & Restore",
                icon = Icons.Default.Backup,
                onClick = {
                    // Navigate directly to backup/restore screen
                    onNavigateToDatabaseInfo()
                    // Note: In a real implementation, you might want to navigate
                    // directly to the backup/restore screen instead of going through
                    // the database info screen. This would require a new navigation function.
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "SQLitePatient3 v1.0",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Patients",
                    count = 42,
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
                StatCard(
                    title = "Events",
                    count = 128,
                    icon = Icons.Default.Event,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
                StatCard(
                    title = "Facilities",
                    count = 7,
                    icon = Icons.Default.Business,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            ActionButton(
                text = "Add New Patient",
                icon = Icons.Default.PersonAdd,
                onClick = {}
            )

            ActionButton(
                text = "Schedule Event",
                icon = Icons.Default.AddTask,
                onClick = {}
            )

            ActionButton(
                text = "Import/Export Data",
                icon = Icons.Default.SwapHoriz,
                onClick = {}
            )

            // Database Management in Preview
            Text(
                text = "Database Management",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            ActionButton(
                text = "Database Information",
                icon = Icons.Default.Storage,
                onClick = {}
            )

            ActionButton(
                text = "Backup & Restore",
                icon = Icons.Default.Backup,
                onClick = {}
            )
        }
    }
}