package com.example.sqlitepatient3.domain.repository

import com.example.sqlitepatient3.domain.model.DiagnosticCode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for accessing diagnostic code data.
 */
interface DiagnosticCodeRepository {
    // Read operations
    fun getAllDiagnosticCodes(): Flow<List<DiagnosticCode>>
    fun getBillableDiagnosticCodes(): Flow<List<DiagnosticCode>>
    fun getCommonDiagnosticCodes(): Flow<List<DiagnosticCode>>
    fun searchDiagnosticCodes(query: String): Flow<List<DiagnosticCode>>

    suspend fun getDiagnosticCodeById(id: Long): DiagnosticCode?
    suspend fun getDiagnosticCodeByIcdCode(icdCode: String): DiagnosticCode?
    suspend fun getDiagnosticCodeCount(): Int

    // Write operations
    suspend fun insertDiagnosticCode(code: DiagnosticCode): Long
    suspend fun insertDiagnosticCodes(codes: List<DiagnosticCode>): List<Long>
    suspend fun updateDiagnosticCode(code: DiagnosticCode)
    suspend fun deleteDiagnosticCode(code: DiagnosticCode)

    suspend fun updateCommonCodeStatus(codeId: Long, isCommon: Boolean): Boolean

    // Bulk operations
    suspend fun deleteAllDiagnosticCodes()
}