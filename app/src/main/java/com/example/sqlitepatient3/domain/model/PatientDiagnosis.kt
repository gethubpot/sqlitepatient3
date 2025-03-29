package com.example.sqlitepatient3.domain.model

import java.time.LocalDate

/**
 * Domain model representing a patient's diagnosis in the application.
 * This associates a patient with an ICD-10 code and stores diagnosis-specific information.
 */
data class PatientDiagnosis(
    val id: Long = 0,
    val patientId: Long,                // Which patient this diagnosis belongs to
    val icdCode: String,                // Direct ICD-10 code (not a foreign key)
    val priority: Int,                  // Position 1-10 (with 1 being primary for regular billing)
    val isHospiceCode: Boolean = false, // Flag for hospice diagnosis
    val diagnosisDate: LocalDate? = null, // When the diagnosis was made
    val resolvedDate: LocalDate? = null,  // When/if condition was resolved
    val notes: String? = null,          // Clinical notes about this diagnosis
    val active: Boolean = true          // Is this diagnosis currently active
)