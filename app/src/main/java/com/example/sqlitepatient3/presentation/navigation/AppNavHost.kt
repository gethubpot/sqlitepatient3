package com.example.sqlitepatient3.presentation.navigation

// Import the new screen
// Removed unused LocalActivity import if it was there
// Import the actual AddEdit screen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sqlitepatient3.presentation.screens.database.BackupRestoreScreen
import com.example.sqlitepatient3.presentation.screens.database.DatabaseInfoScreen
import com.example.sqlitepatient3.presentation.screens.diagnosis.AddEditDiagnosticCodeScreen
import com.example.sqlitepatient3.presentation.screens.diagnosis.DiagnosticCodeListScreen
import com.example.sqlitepatient3.presentation.screens.event.AddEditEventScreen
import com.example.sqlitepatient3.presentation.screens.event.EventDetailScreen
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
    startDestination: String = ScreenRoute.Home.route
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
                onNavigateToDatabaseInfo = { navController.navigate(ScreenRoute.DatabaseInfo.route) },
                onNavigateToDiagnosticCodes = { navController.navigate(ScreenRoute.DiagnosticCodeList.route) }
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
            PatientDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onEditPatient = { navPatientId -> navController.navigate(ScreenRoute.AddEditPatient.createRoute(navPatientId)) },
                onAddEvent = { navPatientId -> navController.navigate(ScreenRoute.AddEditEvent.createRoute(patientId = navPatientId)) },
                onViewAllEvents = { navPatientId -> navController.navigate(ScreenRoute.EventList.route /* + filter? */) },
                onViewDiagnoses = { /* TODO: Navigate to PatientDiagnosisList screen */
                    // Example: navController.navigate(ScreenRoute.PatientDiagnosisList.createRoute(navPatientId))
                }
            )
        }

        composable(
            route = ScreenRoute.AddEditPatient.route,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            // codeId is handled by ViewModel's SavedStateHandle, no need to extract here
            AddEditPatientScreen(
                // patientId parameter was removed from AddEditPatientScreen signature
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

        // Facility Screens (TODOs)
        // TODO: Add facility screen composables (List, Detail, Add/Edit)

        // Event Screens
        composable(
            route = ScreenRoute.AddEditEvent.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("patientId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            // eventId and patientId handled by ViewModel's SavedStateHandle
            AddEditEventScreen(
                onNavigateUp = { navController.navigateUp() },
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

        composable(
            route = ScreenRoute.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            EventDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onEditEvent = { navEventId -> navController.navigate(ScreenRoute.AddEditEvent.createRoute(eventId = navEventId)) },
                onPatientClick = { navPatientId -> navController.navigate(ScreenRoute.PatientDetail.createRoute(navPatientId)) }
            )
        }

        // --- Diagnostic Code Screens ---
        composable(route = ScreenRoute.DiagnosticCodeList.route) {
            DiagnosticCodeListScreen(
                onNavigateUp = { navController.navigateUp() },
                onCodeClick = { codeId ->
                    // Navigate to Add/Edit screen, passing optional codeId
                    navController.navigate(ScreenRoute.AddEditDiagnosticCode.createRoute(codeId))
                }
            )
        }

        composable( // <<<--- CORRECTED BLOCK
            route = ScreenRoute.AddEditDiagnosticCode.route,
            arguments = listOf(navArgument("codeId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            // codeId is handled by the ViewModel's SavedStateHandle
            AddEditDiagnosticCodeScreen(
                onNavigateUp = { navController.navigateUp() },
                onSaveComplete = { navController.navigateUp() }
                // ViewModel is automatically provided by Hilt
            )
        }
        // --- END Diagnostic Code Screens ---

        // --- TODO: Patient Diagnosis Screens ---
        // composable(route = ScreenRoute.PatientDiagnosisList.route) { ... }
        // composable(route = ScreenRoute.AddEditPatientDiagnosis.route) { ... }
        // --- END TODO ---

        // Settings Screen (TODO)
        // TODO: Add settings screen composable
    }
}