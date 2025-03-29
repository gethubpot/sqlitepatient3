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
    object DataImport : ScreenRoute("data_import")
    object DataExport : ScreenRoute("data_export")
    object Settings : ScreenRoute("settings")
    object DatabaseInfo : ScreenRoute("database_info")
    object BackupRestore : ScreenRoute("backup_restore")

    // New routes for diagnoses
    object PatientDiagnosisList : ScreenRoute("patient_diagnoses/{patientId}") {
        fun createRoute(patientId: Long) = "patient_diagnoses/$patientId"
    }
    object AddEditPatientDiagnosis : ScreenRoute("add_edit_diagnosis?diagnosisId={diagnosisId}&patientId={patientId}") {
        fun createRoute(diagnosisId: Long? = null, patientId: Long? = null): String {
            return buildString {
                append("add_edit_diagnosis")
                if (diagnosisId != null || patientId != null) {
                    append("?")
                    if (diagnosisId != null) {
                        append("diagnosisId=$diagnosisId")
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
    object DiagnosticCodeList : ScreenRoute("diagnostic_codes")
    object AddEditDiagnosticCode : ScreenRoute("add_edit_diagnostic_code?codeId={codeId}") {
        fun createRoute(codeId: Long? = null) = codeId?.let { "add_edit_diagnostic_code?codeId=$it" } ?: "add_edit_diagnostic_code"
    }
}