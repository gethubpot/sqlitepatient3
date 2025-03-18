package com.example.sqlitepatient3.presentation.navigation

/**
 * Screen routes for navigation.
 */
sealed class ScreenRoute(val route: String) {
    object Home : ScreenRoute("home")
    object PatientList : ScreenRoute("patient_list")
    object PatientDetail : ScreenRoute("patient_detail/{patientId}") {
        fun createRoute(patientId: Long) = "patient_detail/$patientId"
    }
    object AddEditPatient : ScreenRoute("add_edit_patient?patientId={patientId}") {
        fun createRoute(patientId: Long? = null) = patientId?.let { "add_edit_patient?patientId=$it" } ?: "add_edit_patient"
    }
    object FacilityList : ScreenRoute("facility_list")
    object FacilityDetail : ScreenRoute("facility_detail/{facilityId}") {
        fun createRoute(facilityId: Long) = "facility_detail/$facilityId"
    }
    object AddEditFacility : ScreenRoute("add_edit_facility?facilityId={facilityId}") {
        fun createRoute(facilityId: Long? = null) = facilityId?.let { "add_edit_facility?facilityId=$it" } ?: "add_edit_facility"
    }
    object EventList : ScreenRoute("event_list")
    object EventDetail : ScreenRoute("event_detail/{eventId}") {
        fun createRoute(eventId: Long) = "event_detail/$eventId"
    }
    object AddEditEvent : ScreenRoute("add_edit_event?eventId={eventId}&patientId={patientId}") {
        fun createRoute(eventId: Long? = null, patientId: Long? = null): String {
            return buildString {
                append("add_edit_event")
                if (eventId != null || patientId != null) {
                    append("?")
                    if (eventId != null) {
                        append("eventId=$eventId")
                        if (patientId != null) {
                            append("&")
                        }
                    }
                    if (patientId != null) {
                        append("patientId=$patientId")
                    }
                }
            }
        }
    }
    object ImportExport : ScreenRoute("import_export")
    object Settings : ScreenRoute("settings")
    object DatabaseInfo : ScreenRoute("database_info")
    object BackupRestore : ScreenRoute("backup_restore")  // Added new route for backup/restore screen
}