package com.example.sqlitepatient3.domain.model

/**
 * Domain model representing a healthcare facility or provider in the application.
 * This class is independent of any framework or database implementation.
 */
data class Facility(
    val id: Long = 0,
    val name: String,
    val entityType: String? = null,
    val lastName: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val suffix: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val phoneNumber: String? = null,
    val faxNumber: String? = null,
    val email: String? = null,
    val npi: String? = null,
    val isActive: Boolean = true,
    val facilityCode: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns a display name appropriate for UI presentation.
     * Uses organizational name for entity types, or formats individual name components.
     */
    fun getDisplayName(): String {
        return when {
            name.isNotBlank() -> name
            !lastName.isNullOrBlank() -> buildString {
                append(lastName)
                firstName?.let { append(", $it") }
                middleName?.let { append(" $it") }
                suffix?.let { append(", $it") }
            }
            else -> "Unknown Provider"
        }
    }

    /**
     * Returns a formatted address string for display purposes.
     */
    fun getFormattedAddress(): String {
        return buildString {
            address1?.let { append(it) }
            address2?.let { append("\n$it") }
            append("\n")
            city?.let { append("$it, ") }
            state?.let { append("$it ") }
            zipCode?.let { append(it) }
        }.trim()
    }
}