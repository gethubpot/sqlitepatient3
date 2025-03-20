package com.example.sqlitepatient3.presentation.screens.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.presentation.components.AddFab
import com.example.sqlitepatient3.presentation.components.EmptyStateScaffold
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import com.example.sqlitepatient3.ui.theme.SQLitePatient3Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onNavigateUp: () -> Unit,
    onPatientClick: (Long) -> Unit,
    onAddNewPatient: () -> Unit,
    viewModel: PatientListViewModel = hiltViewModel()
) {
    val patients by viewModel.patients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSortOption by viewModel.currentSortOption.collectAsState()
    val showHospiceOnly by viewModel.showHospiceOnly.collectAsState()
    val showCcmOnly by viewModel.showCcmOnly.collectAsState()
    val showPsychOnly by viewModel.showPsychOnly.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    // Handle loading state
    if (isLoading && patients.isEmpty()) {
        LoadingScaffold(
            title = "Patients",
            onNavigateUp = onNavigateUp
        )
        return
    }

    // Handle empty state
    if (patients.isEmpty()) {
        EmptyStateScaffold(
            title = "Patients",
            emptyMessage = if (searchQuery.isBlank()) {
                "No patients found. Add a new patient to get started."
            } else {
                "No patients match your search."
            },
            onNavigateUp = onNavigateUp,
            onAddClick = onAddNewPatient
        )
        return
    }

    // Main content
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patients (${patients.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AddFab(onClick = onAddNewPatient)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search patients...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                singleLine = true
            )

            // Filters indicator
            if (showHospiceOnly || showCcmOnly || showPsychOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Filters: ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (showHospiceOnly) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleHospiceFilter(false) },
                            label = { Text("Hospice") }
                        )
                    }
                    if (showCcmOnly) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleCcmFilter(false) },
                            label = { Text("CCM") }
                        )
                    }
                    if (showPsychOnly) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.togglePsychFilter(false) },
                            label = { Text("Psych") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Patient list
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(patients) { patient ->
                    PatientListItem(
                        patient = patient,
                        onClick = { onPatientClick(patient.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Patients") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showHospiceOnly,
                            onCheckedChange = { viewModel.toggleHospiceFilter(it) }
                        )
                        Text("Hospice Only")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showCcmOnly,
                            onCheckedChange = { viewModel.toggleCcmFilter(it) }
                        )
                        Text("CCM Only")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showPsychOnly,
                            onCheckedChange = { viewModel.togglePsychFilter(it) }
                        )
                        Text("Psych Only")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleHospiceFilter(false)
                        viewModel.toggleCcmFilter(false)
                        viewModel.togglePsychFilter(false)
                        showFilterDialog = false
                    }
                ) {
                    Text("Clear All")
                }
            }
        )
    }

    // Sort Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Patients") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(PatientListViewModel.SortOption.NAME_ASC) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == PatientListViewModel.SortOption.NAME_ASC,
                            onClick = { viewModel.setSortOption(PatientListViewModel.SortOption.NAME_ASC) }
                        )
                        Text(
                            text = "Name (A-Z)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(PatientListViewModel.SortOption.NAME_DESC) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == PatientListViewModel.SortOption.NAME_DESC,
                            onClick = { viewModel.setSortOption(PatientListViewModel.SortOption.NAME_DESC) }
                        )
                        Text(
                            text = "Name (Z-A)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(PatientListViewModel.SortOption.DATE_ADDED_ASC) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == PatientListViewModel.SortOption.DATE_ADDED_ASC,
                            onClick = { viewModel.setSortOption(PatientListViewModel.SortOption.DATE_ADDED_ASC) }
                        )
                        Text(
                            text = "Date Added (Oldest First)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(PatientListViewModel.SortOption.DATE_ADDED_DESC) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == PatientListViewModel.SortOption.DATE_ADDED_DESC,
                            onClick = { viewModel.setSortOption(PatientListViewModel.SortOption.DATE_ADDED_DESC) }
                        )
                        Text(
                            text = "Date Added (Newest First)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun PatientListItem(
    patient: Patient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${patient.lastName}, ${patient.firstName}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = patient.upi,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                patient.dateOfBirth?.let {
                    Text(
                        text = " • ${it.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Tags/Icons
        Row {
            if (patient.isHospice) {
                TagIcon(color = MaterialTheme.colorScheme.primary, text = "H")
            }
            if (patient.onCcm) {
                TagIcon(color = MaterialTheme.colorScheme.secondary, text = "C")
            }
            if (patient.onPsych) {
                TagIcon(color = MaterialTheme.colorScheme.tertiary, text = "P")
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TagIcon(
    color: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = text,
                color = color,
                modifier = Modifier.padding(2.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PatientListItemPreview() {
    SQLitePatient3Theme {
        PatientListItem(
            patient = Patient(
                id = 1,
                firstName = "John",
                lastName = "Doe",
                upi = "doejoh800101",
                dateOfBirth = LocalDate.of(1980, 1, 1),
                isMale = true,
                isHospice = true,
                onCcm = true,
                onPsych = false,
                facilityId = 1,
                medicareNumber = "123456789A"
            ),
            onClick = {}
        )
    }
}