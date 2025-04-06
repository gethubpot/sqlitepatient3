package com.example.sqlitepatient3.presentation.navigation

import android.app.Activity // Keep this import if you might use activity?.finish()
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
// Import the new screen
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
                // Add navigation trigger for Diagnostic Codes
                onNavigateToDiagnosticCodes = { navController.navigate(ScreenRoute.DiagnosticCodeList.route) } // <<<--- ADDED
            )
        }

        // Patient Screens (Keep existing)
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
            val patientId = backStackEntry.arguments?.getLong("patientId")?.takeIf { it != -1L }
            AddEditPatientScreen(
                patientId = patientId,
                onNavigateUp = { navController.navigateUp() },
                onSaveComplete = { navController.navigateUp() }
            )
        }

        // Database Info Screen (Keep existing)
        composable(route = ScreenRoute.DatabaseInfo.route) {
            DatabaseInfoScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToBackupRestore = { navController.navigate(ScreenRoute.BackupRestore.route) }
            )
        }

        // Backup and Restore Screen (Keep existing)
        composable(route = ScreenRoute.BackupRestore.route) {
            BackupRestoreScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        // Import/Export Screens (Keep existing)
        composable(route = ScreenRoute.ImportExport.route) {
            ImportExportScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToImport = { navController.navigate(ScreenRoute.DataImport.route) },
                onNavigateToExport = { navController.navigate(ScreenRoute.DataExport.route) }
                // You could add another lambda here to navigate to DiagnosticCodeList
                // onNavigateToCodes = { navController.navigate(ScreenRoute.DiagnosticCodeList.route) }
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

        // Facility Screens (Keep existing TODOs)
        // TODO: Add facility screen composables (List, Detail, Add/Edit)

        // Event Screens (Keep existing)
        composable(
            route = ScreenRoute.AddEditEvent.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("patientId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId")?.takeIf { it != -1L }
            val patientId = backStackEntry.arguments?.getLong("patientId")?.takeIf { it != -1L }
            // Removed unused LocalActivity variable

            AddEditEventScreen(
                eventId = eventId,
                patientId = patientId,
                onNavigateUp = {
                    // Simplified navigate up logic (consider if special home navigation is needed)
                    navController.navigateUp()
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

        // --- NEW: Diagnostic Code Screens ---
        composable(route = ScreenRoute.DiagnosticCodeList.route) { // <<<--- ADDED
            DiagnosticCodeListScreen(
                onNavigateUp = { navController.navigateUp() },
                onCodeClick = { codeId ->
                    // Navigate to Add/Edit screen, passing optional codeId
                    navController.navigate(ScreenRoute.AddEditDiagnosticCode.createRoute(codeId))
                }
            )
        }

        composable( // <<<--- ADDED Placeholder
            route = ScreenRoute.AddEditDiagnosticCode.route,
            arguments = listOf(navArgument("codeId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val codeId = backStackEntry.arguments?.getLong("codeId")?.takeIf { it != -1L }
            // TODO: Replace with actual AddEditDiagnosticCodeScreen call when created
            // For now, display a placeholder text
            Box(modifier=Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                Text("Add/Edit Diagnostic Code Screen (ID: $codeId) - TODO")
            }
            // Example structure:
            // AddEditDiagnosticCodeScreen(
            //     codeId = codeId,
            //     onNavigateUp = { navController.navigateUp() },
            //     onSaveComplete = { navController.navigateUp() }
            // )
        }
        // --- END NEW Diagnostic Code Screens ---

        // --- TODO: Patient Diagnosis Screens ---
        // composable(route = ScreenRoute.PatientDiagnosisList.route) { ... }
        // composable(route = ScreenRoute.AddEditPatientDiagnosis.route) { ... }
        // --- END TODO ---


        // Settings Screen (Keep existing TODO)
        // TODO: Add settings screen composable
    }
}