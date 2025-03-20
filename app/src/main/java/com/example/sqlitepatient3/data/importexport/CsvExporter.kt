package com.example.sqlitepatient3.data.importexport

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for exporting data to CSV files
 */
@Singleton
class CsvExporter @Inject constructor(
    private val patientRepository: PatientRepository,
    private val facilityRepository: FacilityRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

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
     * Writes content to a file and returns its Uri
     */
    private fun writeToFile(context: Context, fileName: String, content: String, outputUri: Uri? = null): Uri {
        if (outputUri != null) {
            // Write to user-selected location
            context.contentResolver.openOutputStream(outputUri, "w")?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            }
            return outputUri
        } else {
            // Write to app's external files directory
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: throw IllegalStateException("External files directory not available")

            val file = File(directory, fileName)
            FileWriter(file).use { writer ->
                writer.write(content)
            }

            // Return content URI via FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
    }

    /**
     * Escapes a field for CSV
     * Fields containing commas, double quotes, or newlines need to be quoted
     */
    private fun escapeCsvField(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}