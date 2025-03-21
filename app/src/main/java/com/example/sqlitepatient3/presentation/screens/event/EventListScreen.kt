package com.example.sqlitepatient3.presentation.screens.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.AirplaneTicket
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.EventType
import com.example.sqlitepatient3.domain.model.EventStatus
import com.example.sqlitepatient3.presentation.components.AddFab
import com.example.sqlitepatient3.presentation.components.EmptyStateScaffold
import com.example.sqlitepatient3.presentation.components.LoadingScaffold
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background


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

    // Handle loading state
    if (isLoading && events.isEmpty()) {
        LoadingScaffold(
            title = "Events",
            onNavigateUp = onNavigateUp
        )
        return
    }

    // Handle empty state
    if (events.isEmpty()) {
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

    // Main content
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events (${events.size})") },
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
            AddFab(onClick = onAddNewEvent)
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
                placeholder = { Text("Search events...") },
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

            // Active filters indicator
            if (filterEventType != null || filterStatus != null) {
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
                    filterEventType?.let {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setFilterEventType(null) },
                            label = { Text(it.toString()) }
                        )
                    }
                    filterStatus?.let {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setFilterStatus(null) },
                            label = { Text(it.toString()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Event list
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(events) { event ->
                    EventListItem(
                        event = event,
                        onClick = { onEventClick(event.id) },
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
            title = { Text("Filter Events") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Event Type",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Event Type filters
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setFilterEventType(null) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = filterEventType == null,
                                    onClick = { viewModel.setFilterEventType(null) }
                                )
                                Text(
                                    text = "All Types",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        items(EventType.values().toList()) { eventType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setFilterEventType(eventType) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = filterEventType == eventType,
                                    onClick = { viewModel.setFilterEventType(eventType) }
                                )
                                Text(
                                    text = eventType.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Status filters
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setFilterStatus(null) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = filterStatus == null,
                                    onClick = { viewModel.setFilterStatus(null) }
                                )
                                Text(
                                    text = "All Statuses",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        items(EventStatus.values().toList()) { status ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setFilterStatus(status) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = filterStatus == status,
                                    onClick = { viewModel.setFilterStatus(status) }
                                )
                                Text(
                                    text = status.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
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
                        viewModel.setFilterEventType(null)
                        viewModel.setFilterStatus(null)
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
            title = { Text("Sort Events") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(EventListViewModel.SortOption.DATE_DESC) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == EventListViewModel.SortOption.DATE_DESC,
                            onClick = { viewModel.setSortOption(EventListViewModel.SortOption.DATE_DESC) }
                        )
                        Text(
                            text = "Date (Newest First)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(EventListViewModel.SortOption.DATE_ASC) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == EventListViewModel.SortOption.DATE_ASC,
                            onClick = { viewModel.setSortOption(EventListViewModel.SortOption.DATE_ASC) }
                        )
                        Text(
                            text = "Date (Oldest First)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(EventListViewModel.SortOption.TYPE) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == EventListViewModel.SortOption.TYPE,
                            onClick = { viewModel.setSortOption(EventListViewModel.SortOption.TYPE) }
                        )
                        Text(
                            text = "Event Type",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSortOption(EventListViewModel.SortOption.STATUS) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == EventListViewModel.SortOption.STATUS,
                            onClick = { viewModel.setSortOption(EventListViewModel.SortOption.STATUS) }
                        )
                        Text(
                            text = "Status",
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
fun EventListItem(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Event type icon
        EventTypeIcon(event.eventType)

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Event date and time
            Text(
                text = event.eventDateTime.format(dateFormatter) + " at " + event.eventDateTime.format(timeFormatter),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Event type and duration
            Row {
                Text(
                    text = event.eventType.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (event.eventMinutes > 0) {
                    Text(
                        text = " • ${event.eventMinutes} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Patient info if available
            // We'll fetch patient name in the actual implementation
            Text(
                text = "Patient ID: ${event.patientId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status tag
        StatusChip(event.status)

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EventTypeIcon(eventType: EventType) {
    val icon = when (eventType) {
        EventType.FACE_TO_FACE -> Icons.Default.Person
        EventType.CCM -> Icons.Default.MonitorHeart
        EventType.TCM -> Icons.Default.AirplaneTicket
        EventType.HOSPICE -> Icons.Default.Healing
        EventType.HOME_HEALTH -> Icons.Default.Home
        EventType.FOLLOW_UP -> Icons.Default.CheckCircle
        EventType.MEDICATION_REVIEW -> Icons.Default.Medication
        EventType.PSY -> Icons.Default.Psychology
        EventType.DNR -> Icons.Default.DocumentScanner
        EventType.OTHER -> Icons.Default.Event
    }


            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = eventType.toString(),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
}

@Composable
fun StatusChip(status: EventStatus) {
    val (backgroundColor, textColor) = when (status) {
        EventStatus.PENDING -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        EventStatus.COMPLETED -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        EventStatus.BILLED -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        EventStatus.PAID -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary)
        EventStatus.CANCELLED -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        EventStatus.NO_SHOW -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}