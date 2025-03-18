package com.example.sqlitepatient3.presentation.navigation

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
import com.example.sqlitepatient3.presentation.screens.home.HomeScreen
import com.example.sqlitepatient3.presentation.screens.patient.AddEditPatientScreen
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
                onNavigateToFacilityList = { navController.navigate(ScreenRoute.FacilityList.route) },
                onNavigateToEventList = { navController.navigate(ScreenRoute.EventList.route) },
                onNavigateToAddPatient = { navController.navigate(ScreenRoute.AddEditPatient.createRoute()) },
                onNavigateToAddEvent = { navController.navigate(ScreenRoute.AddEditEvent.createRoute()) },
                onNavigateToImportExport = { navController.navigate(ScreenRoute.ImportExport.route) },
                onNavigateToSettings = { navController.navigate(ScreenRoute.Settings.route) },
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
            // PatientDetailScreen(
            //     patientId = patientId,
            //     onNavigateUp = { navController.navigateUp() },
            //     onEditPatient = { navController.navigate(ScreenRoute.AddEditPatient.createRoute(patientId)) },
            //     onAddEvent = { navController.navigate(ScreenRoute.AddEditEvent.createRoute(patientId = patientId)) }
            // )
            // TODO: Implement PatientDetailScreen
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

        // Facility Screens
        // TODO: Add facility screen composables

        // Event Screens
        // TODO: Add event screen composables

        // Others
        // TODO: Add ImportExport and Settings screen composables
    }
}