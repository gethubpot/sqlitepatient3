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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onNavigateUp: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val backups by viewModel.backups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operation by viewModel.currentOperation.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf<File?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle messages from the ViewModel
    val message by viewModel.message.collectAsState()
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
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
            if (!isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { showBackupDialog = true },
                    icon = { Icon(Icons.Default.Backup, contentDescription = "Backup") },
                    text = { Text("Create Backup") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (operation) {
                            BackupRestoreViewModel.Operation.CREATING -> "Creating backup..."
                            BackupRestoreViewModel.Operation.RESTORING -> "Restoring data..."
                            BackupRestoreViewModel.Operation.DELETING -> "Deleting backup..."
                            else -> "Loading..."
                        }
                    )
                }
            } else {
                if (backups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No backups available.\nCreate a backup to protect your data.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Available Backups",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        items(backups) { backup ->
                            BackupItem(
                                backup = backup,
                                onRestore = { showRestoreDialog = backup },
                                onDelete = { showDeleteDialog = backup }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Create Backup Dialog
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = { Text("Create Backup") },
                text = { Text("Do you want to create a new backup of your database?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.createBackup()
                            }
                            showBackupDialog = false
                        }
                    ) {
                        Text("Yes, Create Backup")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Restore Dialog
        showRestoreDialog?.let { backup ->
            AlertDialog(
                onDismissRequest = { showRestoreDialog = null },
                title = { Text("Restore Data") },
                text = {
                    Text(
                        "Are you sure you want to restore data from this backup? " +
                                "This will replace all current data and cannot be undone."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.restoreBackup(backup)
                            }
                            showRestoreDialog = null
                        }
                    ) {
                        Text("Yes, Restore Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Dialog
        showDeleteDialog?.let { backup ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Backup") },
                text = {
                    Text("Are you sure you want to delete this backup? This cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.deleteBackup(backup)
                            }
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BackupItem(
    backup: File,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val timestamp = getBackupTimestamp(backup)
    val formattedDate = formatBackupDate(backup.lastModified())
    val size = formatFileSize(backup.length())

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Backup: $timestamp",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Created: $formattedDate",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Size: $size",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
            }
        }
    }
}

// Helper functions for formatting backup information
private fun getBackupTimestamp(file: File): String {
    val filename = file.name
    val prefix = "sqlitepatient3_backup_"
    val suffix = ".zip"

    return if (filename.startsWith(prefix) && filename.endsWith(suffix)) {
        filename.substring(prefix.length, filename.length - suffix.length)
    } else {
        "Unknown"
    }
}

private fun formatBackupDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return format.format(date)
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}