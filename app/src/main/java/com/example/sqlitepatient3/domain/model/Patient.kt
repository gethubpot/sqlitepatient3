package com.example.sqlitepatient3.domain.model

import java.time.LocalDate

/**
 * Domain model representing a patient in the application.
 * This class is independent of any framework or database implementation.
 */
data class Patient(
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val upi: String,
    val dateOfBirth: LocalDate?,
    val isMale: Boolean,
    val medicareNumber: String,
    val facilityId: Long? = null,
    val isHospice: Boolean = false,
    val onCcm: Boolean = false,
    val onPsych: Boolean = false,
    val onPsyMed: Boolean = false,
    val psyMedReviewDate: LocalDate? = null,
    val hospiceDiagnosisId: Long? = null,  // New field for primary hospice diagnosis
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Generates a UPI (Unique Patient Identifier) based on patient information
         * Format: first 3 letters of lastName + first 3 letters of firstName + YY + MM + DD
         * If any characters are missing, they are replaced with "_"
         */
        fun generateUpi(lastName: String, firstName: String, dateOfBirth: LocalDate?): String {
            // Extract last name prefix (3 chars)
            val lastNamePrefix = lastName.padEnd(3, '_').take(3).lowercase()

            // Extract first name prefix (3 chars)
            val firstNamePrefix = firstName.padEnd(3, '_').take(3).lowercase()

            // Process date of birth
            if (dateOfBirth == null) {
                // If date of birth is missing, use placeholders
                return "${lastNamePrefix}${firstNamePrefix}______"
            }

            val year = (dateOfBirth.year % 100).toString().padStart(2, '0')
            val month = dateOfBirth.monthValue.toString().padStart(2, '0')
            val day = dateOfBirth.dayOfMonth.toString().padStart(2, '0')

            return "$lastNamePrefix$firstNamePrefix$year$month$day"
        }
    }
}