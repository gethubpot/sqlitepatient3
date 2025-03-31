package com.example.sqlitepatient3.presentation.screens.event

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.domain.model.Event // Keep this import
import com.example.sqlitepatient3.domain.model.EventType
import com.example.sqlitepatient3.domain.model.EventStatus
import com.example.sqlitepatient3.presentation.components.AddFab
import com.example.sqlitepatient3.presentation.components.EmptyStateScaffold
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import java.time.format.DateTimeFormatter

// Import the data class from the ViewModel
import com.example.sqlitepatient3.presentation.screens.event.EventListItemData

// Imports for scrolling
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onNavigateUp: () -> Unit,
    onEventClick: (Long) -> Unit,
    onAddNewEvent: () -> Unit,
    viewModel: EventListViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSortOption by viewModel.currentSortOption.collectAsState()
    val filterEventType by viewModel.filterEventType.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    // --- Loading and Empty States ---
    if (isLoading && events.isEmpty()) {
        LoadingScaffold(title = "Events", onNavigateUp = onNavigateUp)
        return
    }

    if (events.isEmpty() && !isLoading) {
        EmptyStateScaffold(
            title = "Events",
            emptyMessage = if (searchQuery.isBlank() && filterEventType == null && filterStatus == null) {
                "No events found. Schedule a new event to get started."
            } else {
                "No events match your search or filters."
            },
            onNavigateUp = onNavigateUp,
            onAddClick = onAddNewEvent
        )
        return
    }

    // --- Main content ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events (${events.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(Icons.Default.Sort, "Sort")
                    }
                }
            )
        },
        floatingActionButton = { AddFab(onClick = onAddNewEvent) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search events, patients, facilities...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Active filters indicator
            if (filterEventType != null || filterStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Filters: ", style = MaterialTheme.typography.bodySmall)
                    filterEventType?.let {
                        FilterChip(selected = true, onClick = { viewModel.setFilterEventType(null) }, label = { Text(it.toString()) })
                    }
                    filterStatus?.let {
                        FilterChip(selected = true, onClick = { viewModel.setFilterStatus(null) }, label = { Text(it.toString()) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Event list
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events, key = { it.event.id }) { itemData ->
                    EventListItem( // Pass the updated itemData
                        itemData = itemData,
                        onClick = { onEventClick(itemData.event.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
            }
        }
    }

    // --- Filter Dialog (Remains the same) ---
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Events") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Event Type", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                    Column {
                        FilterDialogRadioButton(text = "All Types", selected = filterEventType == null, onClick = { viewModel.setFilterEventType(null) })
                        EventType.values().forEach { type -> FilterDialogRadioButton(text = type.toString(), selected = filterEventType == type, onClick = { viewModel.setFilterEventType(type) }) }
                    }
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Status", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
                    Column {
                        FilterDialogRadioButton(text = "All Statuses", selected = filterStatus == null, onClick = { viewModel.setFilterStatus(null) })
                        EventStatus.values().forEach { status -> FilterDialogRadioButton(text = status.toString(), selected = filterStatus == status, onClick = { viewModel.setFilterStatus(status) }) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilterDialog = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { viewModel.setFilterEventType(null); viewModel.setFilterStatus(null); showFilterDialog = false }) { Text("Clear All") } }
        )
    }

    // --- Sort Dialog (Remains the same) ---
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Events") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SortDialogRadioButton(text = "Date (Newest First)", selected = currentSortOption == EventListViewModel.SortOption.DATE_DESC, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.DATE_DESC) })
                    SortDialogRadioButton(text = "Date (Oldest First)", selected = currentSortOption == EventListViewModel.SortOption.DATE_ASC, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.DATE_ASC) })
                    SortDialogRadioButton(text = "Patient Name (A-Z)", selected = currentSortOption == EventListViewModel.SortOption.PATIENT_NAME_ASC, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.PATIENT_NAME_ASC) })
                    SortDialogRadioButton(text = "Patient Name (Z-A)", selected = currentSortOption == EventListViewModel.SortOption.PATIENT_NAME_DESC, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.PATIENT_NAME_DESC) })
                    SortDialogRadioButton(text = "Facility Code (A-Z)", selected = currentSortOption == EventListViewModel.SortOption.FACILITY_CODE_ASC, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.FACILITY_CODE_ASC) })
                    SortDialogRadioButton(text = "Facility Code (Z-A)", selected = currentSortOption == EventListViewModel.SortOption.FACILITY_CODE_DESC, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.FACILITY_CODE_DESC) })
                    SortDialogRadioButton(text = "Event Type", selected = currentSortOption == EventListViewModel.SortOption.TYPE, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.TYPE) })
                    SortDialogRadioButton(text = "Status", selected = currentSortOption == EventListViewModel.SortOption.STATUS, onClick = { viewModel.setSortOption(EventListViewModel.SortOption.STATUS) })
                }
            },
            confirmButton = { TextButton(onClick = { showSortDialog = false }) { Text("Done") } }
        )
    }
}

// Helper for Radio Button Rows in Dialogs (remains the same)
@Composable
private fun FilterDialogRadioButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SortDialogRadioButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}


// *** --- MODIFIED EventListItem --- ***
@Composable
fun EventListItem(
    itemData: EventListItemData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd") // Date formatter

    // Calculate next follow-up date
    val nextFollowUpDate = itemData.event.calculateNextFollowUpDate()

    // Use a Column to stack the name/date line and the notes line vertically
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // Consistent padding
    ) {
        // Row for Name, Facility Code, Date, and Follow-up Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display Last Name, First Name
            Text(
                text = "${itemData.patientLastName}, ${itemData.patientFirstName}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false) // Prevent name taking all space
            )
            // Display Facility Code if available
            itemData.facilityCode?.takeIf { it.isNotBlank() }?.let { code ->
                Text(
                    text = "($code)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                )
            }
            // Display Event Date (mm/dd)
            Text(
                text = itemData.event.eventDateTime.format(dateFormatter),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Display Next Follow-up Date if it exists
            nextFollowUpDate?.let { nextDate ->
                Text(
                    text = "-> ${nextDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // Push chevron icon to the end

            // Chevron icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        } // End of Name/Date/Follow-up Row

        // Add Text composable for the notes below the name/date row
        // Only display if noteText is not null or blank
        if (!itemData.event.noteText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp)) // Add some space between the lines
            Text(
                text = itemData.event.noteText, // Display the notes
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Use a slightly dimmer color for notes
                maxLines = 1, // Ensure it only takes one line
                overflow = TextOverflow.Ellipsis // Add "..." if the text is too long
            )
        }
    } // End of Column
}


// EventTypeIcon - Can likely be removed if not used elsewhere
// @Composable
// fun EventTypeIcon(...) { ... }

// StatusChip - Can likely be removed if not used elsewhere
// @Composable
// fun StatusChip(...) { ... }