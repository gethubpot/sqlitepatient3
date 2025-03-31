package com.example.sqlitepatient3.presentation.navigation

import android.app.Activity // Added import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Added import
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sqlitepatient3.presentation.screens.database.BackupRestoreScreen
import com.example.sqlitepatient3.presentation.screens.database.DatabaseInfoScreen
import com.example.sqlitepatient3.presentation.screens.event.AddEditEventScreen
import com.example.sqlitepatient3.presentation.screens.event.EventListScreen
import com.example.sqlitepatient3.presentation.screens.home.HomeScreen
import com.example.sqlitepatient3.presentation.screens.importexport.DataExportScreen
import com.example.sqlitepatient3.presentation.screens.importexport.DataImportScreen
import com.example.sqlitepatient3.presentation.screens.importexport.ImportExportScreen
import com.example.sqlitepatient3.presentation.screens.patient.AddEditPatientScreen
import com.example.sqlitepatient3.presentation.screens.patient.PatientListScreen
import com.example.sqlitepatient3.presentation.screens.patient.PatientDetailScreen
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    // Set the start destination
    startDestination: String = ScreenRoute.AddEditEvent.route // Changed from ScreenRoute.Home.route
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = ScreenRoute.Home.route) {
            HomeScreen(
                onNavigateToPatientList = { navController.navigate(ScreenRoute.PatientList.route) },
                onNavigateToFacilityList = { /* TODO: Navigate to Facility List */ },
                onNavigateToEventList = { navController.navigate(ScreenRoute.EventList.route) },
                onNavigateToAddPatient = { navController.navigate(ScreenRoute.AddEditPatient.createRoute()) },
                onNavigateToAddEvent = { navController.navigate(ScreenRoute.AddEditEvent.createRoute()) },
                onNavigateToImportExport = { navController.navigate(ScreenRoute.ImportExport.route) },
                onNavigateToSettings = { /* TODO: Navigate to Settings */ },
                onNavigateToDatabaseInfo = { navController.navigate(ScreenRoute.DatabaseInfo.route) }
            )
        }

        // Patient Screens
        composable(route = ScreenRoute.PatientList.route) {
            PatientListScreen(
                onNavigateUp = { navController.navigateUp() },
                onPatientClick = { patientId -> navController.navigate(ScreenRoute.PatientDetail.createRoute(patientId)) },
                onAddNewPatient = { navController.navigate(ScreenRoute.AddEditPatient.createRoute()) }
            )
        }

        composable(
            route = ScreenRoute.PatientDetail.route,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0
            PatientDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onEditPatient = { navPatientId -> navController.navigate(ScreenRoute.AddEditPatient.createRoute(navPatientId)) },
                onAddEvent = { navPatientId -> navController.navigate(ScreenRoute.AddEditEvent.createRoute(patientId = navPatientId)) },
                onViewAllEvents = { navPatientId -> navController.navigate(ScreenRoute.EventList.route /* + filter by patient? */) }, // Consider how to filter EventList
                onViewDiagnoses = { /* TODO: Navigate to diagnoses screen */ }
            )
        }

        composable(
            route = ScreenRoute.AddEditPatient.route,
            arguments = listOf(
                navArgument("patientId") {
                    type = NavType.LongType
                    defaultValue = -1L  // -1 indicates new patient
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: -1L
            AddEditPatientScreen(
                patientId = if (patientId != -1L) patientId else null,
                onNavigateUp = { navController.navigateUp() },
                onSaveComplete = { navController.navigateUp() }
            )
        }

        // Database Info Screen
        composable(route = ScreenRoute.DatabaseInfo.route) {
            DatabaseInfoScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToBackupRestore = { navController.navigate(ScreenRoute.BackupRestore.route) }
            )
        }

        // Backup and Restore Screen
        composable(route = ScreenRoute.BackupRestore.route) {
            BackupRestoreScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        // Import/Export Screens
        composable(route = ScreenRoute.ImportExport.route) {
            ImportExportScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToImport = { navController.navigate(ScreenRoute.DataImport.route) },
                onNavigateToExport = { navController.navigate(ScreenRoute.DataExport.route) }
            )
        }

        composable(route = ScreenRoute.DataImport.route) {
            DataImportScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(route = ScreenRoute.DataExport.route) {
            DataExportScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        // Facility Screens
        // TODO: Add facility screen composables (List, Detail, Add/Edit)

        // Event Screens
        composable(
            route = ScreenRoute.AddEditEvent.route, // This route definition handles the arguments
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.LongType
                    defaultValue = -1L // Default for adding a new event
                    nullable = false
                },
                navArgument("patientId") {
                    type = NavType.LongType
                    defaultValue = -1L // Default for adding a new event without a pre-selected patient
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: -1L
            // val activity = (LocalContext.current as? Activity) // Get activity instance if needed for finish()

            AddEditEventScreen(
                eventId = if (eventId != -1L) eventId else null,
                patientId = if (patientId != -1L) patientId else null,
                onNavigateUp = {
                    // *** CORRECTED NAVIGATION LOGIC ***
                    // Check if this is the start destination (no previous screen)
                    if (navController.previousBackStackEntry == null) {
                        // Option 1: Navigate explicitly to Home screen
                        navController.navigate(ScreenRoute.Home.route) {
                            // Pop AddEditEvent off the stack so back button on Home exits
                            popUpTo(ScreenRoute.AddEditEvent.route) { inclusive = true }
                            // Avoid multiple instances of Home screen
                            launchSingleTop = true
                        }
                        // Option 2: Finish the activity (close the app)
                        // activity?.finish()
                    } else {
                        // Standard navigate up if not the start destination
                        navController.navigateUp()
                    }
                    // *** END CORRECTION ***
                },
                onSaveComplete = {
                    // Navigate up after saving. If AddEditEvent was the startDestination,
                    // this might now go to Home if the onNavigateUp logic was changed to go there.
                    // Or it might exit if onNavigateUp finishes the activity.
                    // If AddEditEvent was NOT the startDestination, this navigates to the previous screen.
                    navController.navigateUp()
                }
            )
        }

        composable(route = ScreenRoute.EventList.route) {
            EventListScreen(
                onNavigateUp = { navController.navigateUp() },
                onEventClick = { navEventId -> navController.navigate(ScreenRoute.EventDetail.createRoute(navEventId)) },
                onAddNewEvent = { navController.navigate(ScreenRoute.AddEditEvent.createRoute()) }
            )
        }

        // Event Detail Route
        composable(
            route = ScreenRoute.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0
            // TODO: Implement EventDetailScreen using eventId
            // For now, just shows placeholder text and navigates back
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Event Detail Screen for Event ID: $eventId (Not Implemented)")
                Button(onClick = { navController.navigateUp() }) {
                    Text("Back")
                }
            }
//            LaunchedEffect(Unit) {
//                navController.navigateUp() // Or implement the actual screen
//            }
        }

        // Settings Screen
        // TODO: Add settings screen composable
    }
}