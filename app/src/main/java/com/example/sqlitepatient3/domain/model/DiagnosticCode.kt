package com.example.sqlitepatient3.domain.model


data class DiagnosticCode(
    val id: Long = 0,
    val icdCode: String,                // The actual ICD-10 code (e.g., "I10")
    val description: String,            // Full description
    val shorthand: String? = null,      // Quick search abbreviation (e.g., "HTN")
    val billable: Boolean = true,       // Flag for billing eligibility
    val commonCode: Int? = null    // Flag for frequently used codes
)