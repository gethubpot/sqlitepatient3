package com.example.sqlitepatient3.data.local.dao

import androidx.room.*
import com.example.sqlitepatient3.data.local.entity.DiagnosticCodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the diagnostic_codes table.
 */
@Dao
interface DiagnosticCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosticCode(code: DiagnosticCodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosticCodes(codes: List<DiagnosticCodeEntity>): List<Long>

    @Update
    suspend fun updateDiagnosticCode(code: DiagnosticCodeEntity)

    @Delete
    suspend fun deleteDiagnosticCode(code: DiagnosticCodeEntity)

    @Query("SELECT * FROM diagnostic_codes WHERE id = :codeId")
    suspend fun getDiagnosticCodeById(codeId: Long): DiagnosticCodeEntity?

    @Query("SELECT * FROM diagnostic_codes WHERE icdCode = :icdCode")
    suspend fun getDiagnosticCodeByIcdCode(icdCode: String): DiagnosticCodeEntity?

    @Query("SELECT * FROM diagnostic_codes ORDER BY icdCode")
    fun getAllDiagnosticCodes(): Flow<List<DiagnosticCodeEntity>>

    @Query("SELECT * FROM diagnostic_codes WHERE billable = 1 ORDER BY icdCode")
    fun getBillableDiagnosticCodes(): Flow<List<DiagnosticCodeEntity>>

    @Query("SELECT * FROM diagnostic_codes WHERE commonCode = 1 ORDER BY icdCode")
    fun getCommonDiagnosticCodes(): Flow<List<DiagnosticCodeEntity>>

    @Query("SELECT * FROM diagnostic_codes WHERE icdCode LIKE :query OR description LIKE :query OR shorthand LIKE :query ORDER BY icdCode")
    fun searchDiagnosticCodes(query: String): Flow<List<DiagnosticCodeEntity>>

    @Query("SELECT COUNT(*) FROM diagnostic_codes")
    suspend fun getDiagnosticCodeCount(): Int

    @Query("DELETE FROM diagnostic_codes")
    suspend fun deleteAllDiagnosticCodes()

    @Transaction
    @Query("UPDATE diagnostic_codes SET commonCode = :isCommon, updatedAt = :timestamp WHERE id = :codeId")
    suspend fun updateCommonCodeStatus(codeId: Long, isCommon: Boolean, timestamp: Long = System.currentTimeMillis())
}