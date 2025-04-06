package com.example.sqlitepatient3.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sqlitepatient3.domain.model.DiagnosticCode

/**
 * Room database entity representing a diagnostic code.
 * This class maps directly to a table in the database.
 */
@Entity(
    tableName = "diagnostic_codes",
    indices = [
        Index(value = ["icdCode"], unique = true),
        Index(value = ["shorthand"]),
        Index(value = ["billable"]),
        Index(value = ["commonCode"])
    ]
)
data class DiagnosticCodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val icdCode: String,
    val description: String,
    val shorthand: String?,
    val billable: Boolean,
    val commonCode: Int?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): DiagnosticCode {
        return DiagnosticCode(
            id = id,
            icdCode = icdCode,
            description = description,
            shorthand = shorthand,
            billable = billable,
            commonCode = commonCode
        )
    }

    companion object {
        /**
         * Convert from Domain model to Entity
         */
        fun fromDomainModel(code: DiagnosticCode): DiagnosticCodeEntity {
            return DiagnosticCodeEntity(
                id = code.id,
                icdCode = code.icdCode,
                description = code.description,
                shorthand = code.shorthand,
                billable = code.billable,
                commonCode = code.commonCode
            )
        }
    }
}