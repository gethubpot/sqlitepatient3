package com.example.sqlitepatient3.presentation.screens.database

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseInfoScreen(
    onNavigateUp: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    viewModel: DatabaseInfoViewModel = hiltViewModel()
) {
    val dbInfo by viewModel.databaseInfo.collectAsState()
    val schemaInfo by viewModel.schemaInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Information") },
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
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Database Overview Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Database Overview",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                InfoRow("Database Version:", "${dbInfo.version}")

                                if (dbInfo.lastMigrationDate != null) {
                                    InfoRow("Last Migration:", dbInfo.lastMigrationDate)
                                }

                                InfoRow("Database Size:", dbInfo.databaseSizeFormatted)

                                if (dbInfo.walSizeBytes > 0) {
                                    InfoRow("WAL Size:", dbInfo.walSizeFormatted)
                                }

                                InfoRow("Tables:", "${dbInfo.tableCount}")
                                InfoRow("Indices:", "${dbInfo.indexCount}")

                                if (dbInfo.lastMaintenanceDate != null) {
                                    InfoRow("Last Maintenance:", dbInfo.lastMaintenanceDate)
                                }

                                InfoRow(
                                    "Integrity Check:",
                                    if (dbInfo.integrityPassed) "Passed" else "Failed",
                                    valueColor = if (dbInfo.integrityPassed)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )

                                if (dbInfo.lastBackupDate != null) {
                                    InfoRow("Last Backup:", dbInfo.lastBackupDate)
                                }
                            }
                        }
                    }

                    // Data Statistics Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Data Statistics",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                InfoRow("Total Patients:", "${dbInfo.patientCount}")
                                InfoRow("Total Events:", "${dbInfo.eventCount}")
                                InfoRow("Total Facilities:", "${dbInfo.facilityCount}")
                            }
                        }
                    }

                    // Schema Information Section
                    item {
                        Text(
                            "Schema Information",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Show each table's schema
                    items(schemaInfo) { tableInfo ->
                        SchemaTableCard(tableInfo)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Maintenance Actions Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Maintenance Actions",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { viewModel.runIntegrityCheck() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("Run Integrity Check")
                                }

                                Button(
                                    onClick = { viewModel.runVacuum() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("Run VACUUM")
                                }

                                Button(
                                    onClick = { viewModel.runAnalyze() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("Run ANALYZE")
                                }

                                Button(
                                    onClick = onNavigateToBackupRestore,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("Backup & Restore")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemaTableCard(tableInfo: SchemaTableInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tableInfo.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            // Basic table info always visible
            InfoRow("Rows:", "${tableInfo.rowCount}")

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Columns section
                Text(
                    text = "Columns (${tableInfo.columns.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                tableInfo.columns.forEach { column ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = column.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = column.type,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // Flags
                        Row(modifier = Modifier.weight(1f)) {
                            if (column.primaryKey) {
                                Text(
                                    text = "PK",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (column.notNull) {
                                Text(
                                    text = if (column.primaryKey) " NOT NULL" else "NOT NULL",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                if (tableInfo.indices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Indices section
                    Text(
                        text = "Indices (${tableInfo.indices.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    tableInfo.indices.forEach { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = index.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            if (index.unique) {
                                Text(
                                    text = "UNIQUE",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}