package com.example.sqlitepatient3.data.importexport

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.sqlitepatient3.domain.model.DiagnosticCode
import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.repository.DiagnosticCodeRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientDiagnosisRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.iterator

/**
 * Helper class for handling CSV import/export
 */
@Singleton
class CsvImporter @Inject constructor(
    private val patientRepository: PatientRepository,
    private val facilityRepository: FacilityRepository,
    private val diagnosticCodeRepository: DiagnosticCodeRepository,
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    private val TAG = "CsvImporter"
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    /**
     * Imports patient data from a CSV file
     * Returns a pair of (successCount, errorCount)
     */
    suspend fun importPatients(context: Context, fileUri: Uri): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            var successCount = 0
            var errorCount = 0

            try {
                // Read CSV content
                val csvContent = readCsvContent(context, fileUri)
                val lines = csvContent.lines().filter { it.isNotBlank() }

                if (lines.isEmpty()) {
                    return@withContext Pair(0, 0)
                }

                // Parse header
                val header = parseCsvLine(lines[0])
                val headerMap =
                    header.mapIndexed { index, columnName -> columnName.trim() to index }.toMap()

                // Check required columns
                if (!headerMap.containsKey("firstName") || !headerMap.containsKey("lastName")) {
                    throw IOException("CSV must contain firstName and lastName columns")
                }

                // Get facility code to ID mapping
                val facilityCodeMap = mutableMapOf<String, Long>()
                facilityRepository.getAllFacilities().firstOrNull()?.forEach { facility ->
                    facility.facilityCode?.let { code ->
                        facilityCodeMap[code] = facility.id
                    }
                }

                // Process each data row
                for (i in 1 until lines.size) {
                    try {
                        val values = parseCsvLine(lines[i])
                        if (values.size < 2) continue // Skip empty lines

                        val firstName = getColumnValue(values, headerMap, "firstName")
                        val lastName = getColumnValue(values, headerMap, "lastName")

                        if (firstName.isBlank() || lastName.isBlank()) {
                            errorCount++
                            continue
                        }

                        // Optional fields
                        val dateOfBirthStr = getColumnValue(values, headerMap, "dateOfBirth")
                        val dateOfBirth = if (dateOfBirthStr.isNotBlank()) {
                            try {
                                LocalDate.parse(dateOfBirthStr, dateFormatter)
                            } catch (e: DateTimeParseException) {
                                null
                            }
                        } else null

                        val isMale = getColumnValue(values, headerMap, "isMale").equals(
                            "true",
                            ignoreCase = true
                        )

                        val medicareNumber = getColumnValue(values, headerMap, "medicareNumber")

                        val facilityCode = getColumnValue(values, headerMap, "facilityCode")
                        val facilityId =
                            if (facilityCode.isNotBlank()) facilityCodeMap[facilityCode] else null

                        // Additional flags
                        val isHospice = getColumnValue(values, headerMap, "isHospice").equals(
                            "true",
                            ignoreCase = true
                        )
                        val onCcm = getColumnValue(values, headerMap, "onCcm").equals(
                            "true",
                            ignoreCase = true
                        )
                        val onPsych = getColumnValue(values, headerMap, "onPsych").equals(
                            "true",
                            ignoreCase = true
                        )
                        val onPsyMed = getColumnValue(values, headerMap, "onPsyMed").equals(
                            "true",
                            ignoreCase = true
                        )

                        // Insert the patient with all required parameters
                        val patientId = patientRepository.insertPatient(
                            firstName = firstName,
                            lastName = lastName,
                            dateOfBirth = dateOfBirth,
                            isMale = isMale,
                            facilityId = facilityId,
                            medicareNumber = medicareNumber,
                            isHospice = isHospice,
                            onCcm = onCcm,
                            onPsych = onPsych,
                            onPsyMed = onPsyMed
                        )

                        successCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing patient row ${i + 1}", e)
                        errorCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing patients", e)
                throw e
            }

            return@withContext Pair(successCount, errorCount)
        }

    /**
     * Imports facility data from a CSV file
     * Returns a pair of (successCount, errorCount)
     */
    suspend fun importFacilities(context: Context, fileUri: Uri): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            var successCount = 0
            var errorCount = 0

            try {
                // Read CSV content
                val csvContent = readCsvContent(context, fileUri)
                val lines = csvContent.lines().filter { it.isNotBlank() }

                if (lines.isEmpty()) {
                    return@withContext Pair(0, 0)
                }

                // Parse header
                val header = parseCsvLine(lines[0])
                val headerMap =
                    header.mapIndexed { index, columnName -> columnName.trim() to index }.toMap()

                // Check required columns
                if (!headerMap.containsKey("name")) {
                    throw IOException("CSV must contain name column")
                }

                // Process each data row
                for (i in 1 until lines.size) {
                    try {
                        val values = parseCsvLine(lines[i])
                        if (values.size < 1) continue // Skip empty lines

                        val name = getColumnValue(values, headerMap, "name")

                        if (name.isBlank()) {
                            errorCount++
                            continue
                        }

                        // Optional fields
                        val facilityCode = getColumnValue(values, headerMap, "facilityCode")
                        val address1 = getColumnValue(values, headerMap, "address1")
                        val address2 = getColumnValue(values, headerMap, "address2")
                        val city = getColumnValue(values, headerMap, "city")
                        val state = getColumnValue(values, headerMap, "state")
                        val zipCode = getColumnValue(values, headerMap, "zipCode")
                        val phoneNumber = getColumnValue(values, headerMap, "phoneNumber")
                        val faxNumber = getColumnValue(values, headerMap, "faxNumber")
                        val email = getColumnValue(values, headerMap, "email")
                        val isActive = getColumnValue(values, headerMap, "isActive").equals(
                            "true",
                            ignoreCase = true
                        )

                        // Create facility object
                        val facility = Facility(
                            name = name,
                            facilityCode = if (facilityCode.isNotBlank()) facilityCode else null,
                            address1 = if (address1.isNotBlank()) address1 else null,
                            address2 = if (address2.isNotBlank()) address2 else null,
                            city = if (city.isNotBlank()) city else null,
                            state = if (state.isNotBlank()) state else null,
                            zipCode = if (zipCode.isNotBlank()) zipCode else null,
                            phoneNumber = if (phoneNumber.isNotBlank()) phoneNumber else null,
                            faxNumber = if (faxNumber.isNotBlank()) faxNumber else null,
                            email = if (email.isNotBlank()) email else null,
                            isActive = isActive
                        )

                        // Insert facility
                        facilityRepository.insertFacility(facility)

                        successCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing facility row ${i + 1}", e)
                        errorCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing facilities", e)
                throw e
            }

            return@withContext Pair(successCount, errorCount)
        }

    /**
     * Imports diagnosis data from a CSV file
     * Returns a triple of (diagnosisSuccessCount, diagnosisErrorCount, unmatchedPatientCount)
     */
    suspend fun importDiagnoses(context: Context, fileUri: Uri): Triple<Int, Int, Int> =
        withContext(Dispatchers.IO) {
            var diagnosisSuccessCount = 0
            var diagnosisErrorCount = 0
            var unmatchedPatientCount = 0

            try {
                // Read CSV content
                val csvContent = readCsvContent(context, fileUri)
                val lines = csvContent.lines().filter { it.isNotBlank() }

                if (lines.isEmpty()) {
                    return@withContext Triple(0, 0, 0)
                }

                // Parse header
                val header = parseCsvLine(lines[0])
                val headerMap =
                    header.mapIndexed { index, columnName -> columnName.trim() to index }.toMap()

                // Check required columns
                if (!headerMap.containsKey("patientUPI") || !headerMap.containsKey("icdCode") || !headerMap.containsKey("description")) {
                    throw IOException("CSV must contain patientUPI, icdCode, and description columns")
                }

                // Create a cache of patients by UPI for faster lookup
                val patientsCache = mutableMapOf<String, Long>()
                patientRepository.getAllPatients().firstOrNull()?.forEach { patient ->
                    patientsCache[patient.upi] = patient.id
                }

                // Create a cache of existing diagnostic codes to avoid duplicates
                val existingCodesCache = mutableMapOf<String, Long>()
                diagnosticCodeRepository.getAllDiagnosticCodes().firstOrNull()?.forEach { code ->
                    existingCodesCache[code.icdCode] = code.id
                }

                // Process each data row
                for (i in 1 until lines.size) {
                    try {
                        val values = parseCsvLine(lines[i])
                        if (values.size < 3) continue // Skip empty lines

                        val patientUPI = getColumnValue(values, headerMap, "patientUPI")
                        var icdCodeRaw = getColumnValue(values, headerMap, "icdCode")
                        val icdCode = icdCodeRaw.replace(".", "")
                        val description = getColumnValue(values, headerMap, "description")

                        // Skip if any required field is blank
                        if (patientUPI.isBlank() || icdCode.isBlank()) {
                            diagnosisErrorCount++
                            continue
                        }

                        // Find patient ID by UPI
                        val patientId = patientsCache[patientUPI]
                        if (patientId == null) {
                            unmatchedPatientCount++
                            continue
                        }

                        // Optional fields
                        val priorityStr = getColumnValue(values, headerMap, "priority")
                        val priority = priorityStr.toIntOrNull() ?: 1

                        val isHospiceCode = getColumnValue(values, headerMap, "isHospiceCode").equals(
                            "true",
                            ignoreCase = true
                        )

                        val active = getColumnValue(values, headerMap, "active").equals(
                            "true",
                            ignoreCase = true
                        )

                        // Look up or create the diagnostic code
                        val diagnosticCodeId = if (existingCodesCache.containsKey(icdCode)) { // Use modified code
                            existingCodesCache[icdCode]!!
                        } else {
                            // Create a new diagnostic code
                            val newCodeId = diagnosticCodeRepository.insertDiagnosticCode(
                                DiagnosticCode(
                                    icdCode = icdCode, // Use modified code
                                    description = description,
                                    shorthand = null,
                                    billable = true,
                                    commonCode = null
                                )
                            )
                            // Add to cache
                            existingCodesCache[icdCode] = newCodeId
                            newCodeId
                        }

                        // Insert the patient diagnosis
                        patientDiagnosisRepository.insertPatientDiagnosis(
                            patientId = patientId,
                            icdCode = icdCode,
                            priority = priority,
                            isHospiceCode = isHospiceCode,
                            diagnosisDate = LocalDate.now(), // Current date as diagnosis date
                            notes = null
                        )

                        // If this is a hospice diagnosis and marked as a hospice code, update patient
                        if (isHospiceCode) {
                            val patient = patientRepository.getPatientById(patientId)
                            if (patient != null && (patient.hospiceDiagnosisId == null || priority == 1)) {
                                // This is either the first hospice diagnosis or the primary diagnosis
                                // Get the diagnosis ID we just inserted
                                val diagnosis = patientDiagnosisRepository.getPatientDiagnosisByPriority(patientId, priority)
                                diagnosis?.let {
                                    patientRepository.updatePatientHospiceDiagnosis(patientId, it.id)
                                }
                            }
                        }

                        diagnosisSuccessCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing diagnosis row ${i + 1}", e)
                        diagnosisErrorCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing diagnoses", e)
                throw e
            }

            return@withContext Triple(diagnosisSuccessCount, diagnosisErrorCount, unmatchedPatientCount)
        }

    /**
     * Helper function to read CSV content from a URI
     */
    private fun readCsvContent(context: Context, fileUri: Uri): String {
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            return reader.readText()
        } ?: throw IOException("Could not open file")
    }

    /**
     * Safely gets a column value or returns empty string if column doesn't exist
     */
    private fun getColumnValue(
        values: List<String>,
        headerMap: Map<String, Int>,
        columnName: String
    ): String {
        val index = headerMap[columnName] ?: return ""
        return if (index < values.size) values[index].trim() else ""
    }

    /**
     * Parse a CSV line, handling quoted fields
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        var currentValue = StringBuilder()

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(currentValue.toString().trim())
                    currentValue = StringBuilder()
                }

                else -> currentValue.append(char)
            }
        }

        // Add the last field
        result.add(currentValue.toString().trim())
        return result
    }
}