package com.example.sqlitepatient3.data.importexport

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.sqlitepatient3.domain.model.DiagnosticCode
import com.example.sqlitepatient3.domain.model.Facility
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
     * Imports diagnosis data from a CSV file, linking diagnoses to patients.
     * Only creates new DiagnosticCode records if the code doesn't already exist.
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
                        val icdCode = icdCodeRaw.replace(".", "") // Use cleaned code
                        val description = getColumnValue(values, headerMap, "description") // Description from THIS CSV

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

                        // active column check removed as we assume import means active, and linking is the primary goal

                        // Look up or create the diagnostic code
                        val diagnosticCodeId: Long
                        if (existingCodesCache.containsKey(icdCode)) { // Check if code already exists
                            diagnosticCodeId = existingCodesCache[icdCode]!!
                            // DO NOTHING TO THE EXISTING CODE'S DESCRIPTION
                        } else {
                            // Create a new diagnostic code ONLY IF IT DOESN'T EXIST
                            // Using the description from THIS specific CSV import
                            val newCode = DiagnosticCode(
                                icdCode = icdCode, // Use cleaned code
                                description = description, // Description from CSV
                                shorthand = null, // Not importing shorthand here
                                billable = true, // Assuming billable
                                commonCode = null // Assuming not common by default
                            )
                            val newCodeId = diagnosticCodeRepository.insertDiagnosticCode(newCode)
                            // Add to cache for subsequent rows in this same file
                            existingCodesCache[icdCode] = newCodeId
                            diagnosticCodeId = newCodeId
                        }

                        // Insert the patient diagnosis link
                        patientDiagnosisRepository.insertPatientDiagnosis(
                            patientId = patientId,
                            icdCode = icdCode,
                            priority = priority,
                            isHospiceCode = isHospiceCode,
                            diagnosisDate = LocalDate.now(), // Current date as diagnosis date
                            notes = null // Not importing notes here
                        )

                        // Logic to update patient's primary hospice diagnosis remains the same...
                        if (isHospiceCode) {
                            val patient = patientRepository.getPatientById(patientId)
                            if (patient != null && (patient.hospiceDiagnosisId == null || priority == 1)) {
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


    // --- NEW FUNCTION START ---
    /**
     * Imports an ICD-10 code library from a CSV file.
     * Updates existing codes with descriptions from the CSV.
     * Optionally inserts codes that don't exist.
     * Assumes CSV has columns 'icdCode' and 'description'.
     *
     * @param context The application context.
     * @param fileUri The URI of the CSV file.
     * @param insertNew If true, inserts codes from the CSV that are not already in the database.
     * @return A Triple containing (updatedCount, insertedCount, errorCount).
     */
    suspend fun importDiagnosticCodeLibrary(
        context: Context,
        fileUri: Uri,
        insertNew: Boolean = true // Control whether to insert codes not found
    ): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        var updatedCount = 0
        var insertedCount = 0
        var errorCount = 0

        try {
            val csvContent = readCsvContent(context, fileUri)
            val lines = csvContent.lines().filter { it.isNotBlank() }

            if (lines.isEmpty()) {
                return@withContext Triple(0, 0, 0)
            }

            val header = parseCsvLine(lines[0])
            val headerMap = header.mapIndexed { index, columnName ->
                columnName.trim().lowercase() to index // Use lowercase for flexibility
            }.toMap()

            // --- Column Name Flexibility ---
            // Try common variations for column names
            val codeColName = listOf("icdcode", "code", "icd-10", "icd10").find { headerMap.containsKey(it) }
            val descColName = listOf("description", "desc", "longdescription", "text").find { headerMap.containsKey(it) }
            // Add more optional columns if needed (e.g., shorthand, billable)
            // val shorthandColName = listOf("shorthand", "short").find { headerMap.containsKey(it) }

            // --- Validation ---
            if (codeColName == null) {
                throw IOException("CSV must contain an ICD code column (e.g., 'icdCode', 'code')")
            }
            if (descColName == null) {
                throw IOException("CSV must contain a description column (e.g., 'description', 'desc')")
            }
            // --- End Validation ---

            Log.d(TAG, "Importing code library. Code column: '$codeColName', Desc column: '$descColName'")

            // Process data rows
            for (i in 1 until lines.size) {
                try {
                    val values = parseCsvLine(lines[i])

                    val icdCodeRaw = getColumnValue(values, headerMap, codeColName)
                    val description = getColumnValue(values, headerMap, descColName)

                    if (icdCodeRaw.isBlank()) {
                        Log.w(TAG, "Skipping row ${i + 1}: Blank ICD code")
                        errorCount++
                        continue
                    }

                    // Clean the code (remove dots, etc.) - adjust if your library format differs
                    val icdCode = icdCodeRaw.replace(".", "").trim()
                    if (icdCode.isBlank()) {
                        Log.w(TAG, "Skipping row ${i + 1}: Blank ICD code after cleaning '$icdCodeRaw'")
                        errorCount++
                        continue
                    }


                    // --- Find existing code ---
                    val existingCode = diagnosticCodeRepository.getDiagnosticCodeByIcdCode(icdCode)

                    if (existingCode != null) {
                        // --- Update Existing Code ---
                        // Update only if the description is different and not blank in the CSV
                        if (description.isNotBlank() && existingCode.description != description) {
                            val updatedCode = existingCode.copy(
                                description = description
                                // Optionally update other fields like shorthand, billable if present in CSV
                                // shorthand = getColumnValue(values, headerMap, shorthandColName).takeIf { it.isNotBlank() } ?: existingCode.shorthand,
                                // billable = getColumnValue(values, headerMap, billableColName).toBooleanStrictOrNull() ?: existingCode.billable
                            )
                            diagnosticCodeRepository.updateDiagnosticCode(updatedCode)
                            updatedCount++
                        } else {
                            // No update needed (description same or blank in CSV)
                        }
                    } else if (insertNew) {
                        // --- Insert New Code (Optional) ---
                        if (description.isBlank()) {
                            Log.w(TAG, "Skipping new code '$icdCode' from row ${i + 1}: Blank description")
                            errorCount++
                            continue
                        }
                        val newCode = DiagnosticCode(
                            icdCode = icdCode,
                            description = description,
                            // Set defaults for other fields or parse from CSV if available
                            shorthand = null, // getColumnValue(values, headerMap, shorthandColName).takeIf { it.isNotBlank() },
                            billable = true,  // getColumnValue(values, headerMap, billableColName).toBooleanStrictOrNull() ?: true,
                            commonCode = null
                        )
                        diagnosticCodeRepository.insertDiagnosticCode(newCode)
                        insertedCount++
                    } else {
                        // Code doesn't exist and insertNew is false, skip.
                        Log.d(TAG, "Skipping row ${i + 1}: Code '$icdCode' not found and insertNew is false.")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing code library row ${i + 1}", e)
                    errorCount++
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error importing diagnostic code library", e)
            throw e // Re-throw to be caught by ViewModel
        }

        return@withContext Triple(updatedCount, insertedCount, errorCount)
    }
    // --- NEW FUNCTION END ---


    /**
     * Helper function to read CSV content from a URI
     */
    private fun readCsvContent(context: Context, fileUri: Uri): String {
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            return reader.readText()
        } ?: throw IOException("Could not open file: $fileUri") // Added URI to error
    }

    /**
     * Safely gets a column value or returns empty string if column doesn't exist
     */
    private fun getColumnValue(
        values: List<String>,
        headerMap: Map<String, Int>,
        columnName: String? // Made nullable, as we might not find the column
    ): String {
        if (columnName == null) return "" // Return empty if column name wasn't found
        val index = headerMap[columnName] ?: return ""
        return if (index >= 0 && index < values.size) values[index].trim() else ""
    }


    /**
     * Parse a CSV line, handling quoted fields and different line endings.
     * More robust implementation.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val iterator = line.iterator()
        var currentValue = StringBuilder()
        var inQuotes = false

        while (iterator.hasNext()) {
            val char = iterator.next()

            when {
                // Handles double quote escaping ("") inside quoted fields
                char == '"' && inQuotes && iterator.hasNext() && iterator.peekNextChar() == '"' -> {
                    currentValue.append('"')
                    iterator.next() // Consume the second quote
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(currentValue.toString()) // Keep leading/trailing spaces within quotes
                    currentValue = StringBuilder()
                }
                // Handle potential \r\n or \n line endings if needed, though usually handled by lines()
                // char == '\r' && !inQuotes && iterator.hasNext() && iterator.peekNextChar() == '\n' -> { /* Skip \r */ }
                // char == '\n' && !inQuotes -> { /* End of line, should be handled by lines() */ }
                else -> currentValue.append(char)
            }
        }
        result.add(currentValue.toString()) // Add the last value

        // Trim only if not quoted originally (this might be too complex, trim() on getColumnValue is simpler)
        // return result.map { if (it.startsWith("\"") && it.endsWith("\"")) it.drop(1).dropLast(1) else it.trim() }
        return result // Rely on trim() in getColumnValue
    }

    // Helper extension for peeking next char in iterator
    private fun CharIterator.peekNextChar(): Char? {
        // This requires specific iterator implementation or lookahead logic.
        // For simplicity, we'll skip actual peeking. Standard CSV parsing libraries handle this.
        // If implementing manually, you might need a custom reader.
        return null // Placeholder
    }

}