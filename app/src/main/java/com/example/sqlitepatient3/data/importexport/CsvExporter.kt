package com.example.sqlitepatient3.data.importexport

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.example.sqlitepatient3.domain.model.*
import com.example.sqlitepatient3.domain.repository.DiagnosticCodeRepository // Assuming you might need this later
import com.example.sqlitepatient3.domain.repository.EventRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientDiagnosisRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

/**
 * Helper class for exporting data to CSV files
 */
@Singleton
class CsvExporter @Inject constructor(
    private val patientRepository: PatientRepository,
    private val facilityRepository: FacilityRepository,
    private val eventRepository: EventRepository,
    private val patientDiagnosisRepository: PatientDiagnosisRepository
    // Assuming DiagnosticCodeRepository might be needed later if you export codes
    // private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm")
    // Removed fileNameFormatter as eventFile is now empty

    /**
     * Exports all patients to a CSV file
     * Returns the Uri of the created file
     */
    suspend fun exportPatients(context: Context, outputUri: Uri? = null): Uri = withContext(Dispatchers.IO) {
        val patients = patientRepository.getAllPatients().first()

        // Prepare the CSV content
        val csvBuilder = StringBuilder()

        // Headers
        csvBuilder.appendLine("firstName,lastName,dateOfBirth,isMale,medicareNumber,facilityCode,isHospice,onCcm,onPsych,onPsyMed")

        // Get facility ID to code mapping
        val facilityMap = mutableMapOf<Long, String?>()
        facilityRepository.getAllFacilities().first().forEach { facility ->
            facilityMap[facility.id] = facility.facilityCode
        }

        // Rows
        patients.forEach { patient ->
            csvBuilder.append(escapeCsvField(patient.firstName)).append(",")
            csvBuilder.append(escapeCsvField(patient.lastName)).append(",")
            csvBuilder.append(escapeCsvField(patient.dateOfBirth?.format(dateFormatter) ?: "")).append(",")
            csvBuilder.append(patient.isMale).append(",")
            csvBuilder.append(escapeCsvField(patient.medicareNumber)).append(",")
            csvBuilder.append(escapeCsvField(patient.facilityId?.let { facilityMap[it] } ?: "")).append(",")
            csvBuilder.append(patient.isHospice).append(",")
            csvBuilder.append(patient.onCcm).append(",")
            csvBuilder.append(patient.onPsych).append(",")
            csvBuilder.append(patient.onPsyMed)
            csvBuilder.appendLine()
        }

        return@withContext writeToFile(context, "patients.csv", csvBuilder.toString(), outputUri)
    }

    /**
     * Exports all facilities to a CSV file
     * Returns the Uri of the created file
     */
    suspend fun exportFacilities(context: Context, outputUri: Uri? = null): Uri = withContext(Dispatchers.IO) {
        val facilities = facilityRepository.getAllFacilities().first()

        // Prepare the CSV content
        val csvBuilder = StringBuilder()

        // Headers
        csvBuilder.appendLine("name,facilityCode,address1,address2,city,state,zipCode,phoneNumber,faxNumber,email,isActive")

        // Rows
        facilities.forEach { facility ->
            csvBuilder.append(escapeCsvField(facility.name)).append(",")
            csvBuilder.append(escapeCsvField(facility.facilityCode ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.address1 ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.address2 ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.city ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.state ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.zipCode ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.phoneNumber ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.faxNumber ?: "")).append(",")
            csvBuilder.append(escapeCsvField(facility.email ?: "")).append(",")
            csvBuilder.append(facility.isActive)
            csvBuilder.appendLine()
        }

        return@withContext writeToFile(context, "facilities.csv", csvBuilder.toString(), outputUri)
    }

    /**
     * Exports all events to a CSV file according to the specified format.
     * Returns the Uri of the created file.
     */
    suspend fun exportEvents(context: Context, outputUri: Uri? = null): Uri = withContext(Dispatchers.IO) {
        // Get all events with related patient information
        val events = eventRepository.getAllEvents().first()

        // Get all patients for UPI lookup
        val patients = patientRepository.getAllPatients().first()
        val patientMap = patients.associateBy { it.id }

        // Get diagnoses for each patient
        val patientDiagnoses = mutableMapOf<Long, List<PatientDiagnosis>>()
        patients.forEach { patient ->
            patientDiagnosisRepository.getActivePatientDiagnoses(patient.id).first().let { diagnoses ->
                if (diagnoses.isNotEmpty()) {
                    patientDiagnoses[patient.id] = diagnoses
                }
            }
        }

        // Prepare the CSV content
        val csvBuilder = StringBuilder()

        // Headers (as per your example CSV structure)
        csvBuilder.appendLine("patientUpi,eventDateTime,eventBillDate,eventMinutes,ccmMinutes,noteText,cptCode,modifier,eventType,status,hospDisDate,ttddDate,monthlyBillingId,diagnoses,eventFile,")

        // Format each event
        events.forEach { event ->
            val patient = patientMap[event.patientId]
            if (patient != null) {
                // 1. Patient UPI
                csvBuilder.append(escapeCsvField(patient.upi)).append(",")

                // 2. Event date time
                val eventDateTime = event.eventDateTime
                csvBuilder.append(escapeCsvField(eventDateTime.format(dateTimeFormatter))).append(",")

                // 3. Event bill date - calculate based on rules
                val billDate = calculateBillDate(event)
                csvBuilder.append(escapeCsvField(billDate.format(dateFormatter))).append(",")

                // 4. Event minutes
                csvBuilder.append(event.eventMinutes).append(",")

                // 5. CCM minutes (only filled if event type is CCM)
                if (event.eventType == EventType.CCM) {
                    csvBuilder.append(event.eventMinutes)
                }
                csvBuilder.append(",")

                // 6. Note text
                csvBuilder.append(escapeCsvField(event.noteText ?: "")).append(",")

                // 7. CPT code - **MODIFIED: Always empty**
                csvBuilder.append("").append(",")

                // 8. Modifier - **MODIFIED: Always empty**
                csvBuilder.append("").append(",")

                // 9. Event type
                csvBuilder.append(escapeCsvField(event.eventType.toString())).append(",")

                // 10. Status - **MODIFIED: Always "PENDING"**
                csvBuilder.append(escapeCsvField("PENDING")).append(",")

                // 11. Hospital discharge date
                csvBuilder.append(escapeCsvField(event.hospDischargeDate?.format(dateFormatter) ?: "")).append(",")

                // 12. ttddDate (follow-up date) - **MODIFIED: Apply user rules**
                val ttddDate = calculateTtddDate(event) // Uses the updated function below
                csvBuilder.append(escapeCsvField(ttddDate.format(dateFormatter))).append(",")

                // 13. Monthly billing ID
                csvBuilder.append(escapeCsvField(event.monthlyBillingId?.toString() ?: "")).append(",")

                // 14. Diagnoses - First patient diagnosis by default
                val diagnosis = patientDiagnoses[event.patientId]?.firstOrNull()?.icdCode ?: ""
                csvBuilder.append(escapeCsvField(diagnosis)).append(",")

                // 15. Event file - **MODIFIED: Always empty**
                csvBuilder.append("").append(",")

                csvBuilder.appendLine()
            }
        }

        return@withContext writeToFile(context, "events.csv", csvBuilder.toString(), outputUri)
    }

    /**
     * Calculate bill date based on the specified rules:
     * - If event date is on or before the 28th of the month: 28th of the same month
     * - If event date is after the 28th: 28th of the next month
     * - For TCM: 32 days after hospital discharge date
     */
    private fun calculateBillDate(event: Event): LocalDate {
        // Special case for TCM event type
        if (event.eventType == EventType.TCM && event.hospDischargeDate != null) {
            return event.hospDischargeDate.plusDays(32)
        }

        val eventDate = event.eventDateTime.toLocalDate()
        val dayOfMonth = eventDate.dayOfMonth

        return if (dayOfMonth <= 28) {
            // On or before 28th - bill date is the 28th of the same month
            LocalDate.of(eventDate.year, eventDate.month, 28)
        } else {
            // After 28th - bill date is the 28th of the next month
            val nextMonth = eventDate.plusMonths(1)
            LocalDate.of(nextMonth.year, nextMonth.month, 28)
        }
    }

    // *** UPDATED FUNCTION ***
    /**
     * Calculate ttddDate (follow-up date) based on user rules:
     * - "None": Same as event date
     * - "Weekly": 1 week after event date
     * - "Monthly": 1 month after event date
     * - "Quarterly": 3 months after event date  // Added
     * - "Semi-Annual": 6 months after event date // Added
     * - "Annual": 1 year after event date      // Added
     * (Uses event.followUpRecurrence to determine the type)
     */
    private fun calculateTtddDate(event: Event): LocalDate {
        val eventDate = event.eventDateTime.toLocalDate()
        return when (event.followUpRecurrence) {
            FollowUpRecurrence.WEEKLY -> eventDate.plusWeeks(1)
            FollowUpRecurrence.MONTHLY -> eventDate.plusMonths(1)
            // *** ADDED CASES ***
            FollowUpRecurrence.QUARTERLY -> eventDate.plusMonths(3)
            FollowUpRecurrence.SEMI_ANNUAL -> eventDate.plusMonths(6)
            FollowUpRecurrence.ANNUAL -> eventDate.plusYears(1)
            // *** END ADDED CASES ***
            else -> eventDate // Default to event date for NONE and any other unhandled cases
        }
    }
    // *** END UPDATED FUNCTION ***


    /**
     * Writes content to a file and returns its Uri
     */
    private fun writeToFile(context: Context, fileName: String, content: String, outputUri: Uri? = null): Uri {
        if (outputUri != null) {
            // Write to user-selected location
            context.contentResolver.openOutputStream(outputUri, "w")?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            } ?: throw IOException("Could not open output stream for URI: $outputUri")
            return outputUri
        } else {
            // Write to app's external files directory
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: throw IllegalStateException("External files directory not available")

            // Ensure directory exists
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, fileName)
            FileWriter(file).use { writer ->
                writer.write(content)
            }

            // Return content URI via FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Make sure this matches your FileProvider authority
                file
            )
        }
    }

    /**
     * Escapes a field for CSV
     * Fields containing commas, double quotes, or newlines need to be quoted
     * Double quotes within the field are escaped by doubling them ("")
     */
    private fun escapeCsvField(value: String): String {
        // Trim leading/trailing whitespace
        val trimmedValue = value.trim()
        // Check if quoting is necessary
        val needsQuoting = trimmedValue.contains(",") || trimmedValue.contains("\"") || trimmedValue.contains("\n")

        return if (needsQuoting) {
            // Escape double quotes within the value
            val escapedValue = trimmedValue.replace("\"", "\"\"")
            // Enclose in double quotes
            "\"$escapedValue\""
        } else {
            trimmedValue // Return the trimmed value as is if no quoting needed
        }
    }
}