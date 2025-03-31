package com.example.sqlitepatient3.presentation.navigation

import android.app.Activity // Keep this import if you might use activity?.finish()
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Keep this import if you might use activity?.finish()
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.compose.LocalActivity
import com.example.sqlitepatient3.presentation.screens.database.BackupRestoreScreen
import com.example.sqlitepatient3.presentation.screens.database.DatabaseInfoScreen
import com.example.sqlitepatient3.presentation.screens.event.AddEditEventScreen
import com.example.sqlitepatient3.presentation.screens.event.EventDetailScreen // <<<--- ADDED IMPORT
import com.example.sqlitepatient3.presentation.screens.event.EventListScreen
import com.example.sqlitepatient3.presentation.screens.home.HomeScreen
import com.example.sqlitepatient3.presentation.screens.importexport.DataExportScreen
import com.example.sqlitepatient3.presentation.screens.importexport.DataImportScreen
import com.example.sqlitepatient3.presentation.screens.importexport.ImportExportScreen
import com.example.sqlitepatient3.presentation.screens.patient.AddEditPatientScreen
import com.example.sqlitepatient3.presentation.screens.patient.PatientDetailScreen
import com.example.sqlitepatient3.presentation.screens.patient.PatientListScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ScreenRoute.Home.route // You can change startDestination back if needed
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
            // Note: PatientDetailScreen fetches its own data using the ID
            PatientDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onEditPatient = { navPatientId -> navController.navigate(ScreenRoute.AddEditPatient.createRoute(navPatientId)) },
                onAddEvent = { navPatientId -> navController.navigate(ScreenRoute.AddEditEvent.createRoute(patientId = navPatientId)) },
                onViewAllEvents = { navPatientId -> navController.navigate(ScreenRoute.EventList.route /* + filter? */) },
                onViewDiagnoses = { /* TODO: Navigate to diagnoses */ }
            )
        }

        composable(
            route = ScreenRoute.AddEditPatient.route,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType; defaultValue = -1L })
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
            route = ScreenRoute.AddEditEvent.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("patientId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: -1L
            val activity = LocalActivity.current

            AddEditEventScreen(
                eventId = if (eventId != -1L) eventId else null,
                patientId = if (patientId != -1L) patientId else null,
                onNavigateUp = {
                    if (navController.previousBackStackEntry == null) {
                        navController.navigate(ScreenRoute.Home.route) {
                            popUpTo(ScreenRoute.AddEditEvent.route) { inclusive = true }
                            launchSingleTop = true
                        }
                        // activity?.finish() // Alternative: close app
                    } else {
                        navController.navigateUp()
                    }
                },
                onSaveComplete = { navController.navigateUp() }
            )
        }

        composable(route = ScreenRoute.EventList.route) {
            EventListScreen(
                onNavigateUp = { navController.navigateUp() },
                onEventClick = { navEventId -> navController.navigate(ScreenRoute.EventDetail.createRoute(navEventId)) },
                onAddNewEvent = { navController.navigate(ScreenRoute.AddEditEvent.createRoute()) }
            )
        }

        // --- MODIFIED Event Detail Route ---
        composable(
            route = ScreenRoute.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            // EventDetailScreen now uses its ViewModel which gets the ID from SavedStateHandle
            EventDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onEditEvent = { navEventId -> navController.navigate(ScreenRoute.AddEditEvent.createRoute(eventId = navEventId)) },
                // Navigate to patient detail when patient card is clicked
                onPatientClick = { navPatientId -> navController.navigate(ScreenRoute.PatientDetail.createRoute(navPatientId)) }
            )
        }
        // --- END MODIFIED Event Detail Route ---

        // Settings Screen
        // TODO: Add settings screen composable
    }
}