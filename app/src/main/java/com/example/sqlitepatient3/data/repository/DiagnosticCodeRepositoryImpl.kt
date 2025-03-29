package com.example.sqlitepatient3.data.repository

import com.example.sqlitepatient3.data.local.dao.DiagnosticCodeDao
import com.example.sqlitepatient3.data.local.entity.DiagnosticCodeEntity
import com.example.sqlitepatient3.domain.model.DiagnosticCode
import com.example.sqlitepatient3.domain.repository.DiagnosticCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticCodeRepositoryImpl @Inject constructor(
    private val diagnosticCodeDao: DiagnosticCodeDao
) : DiagnosticCodeRepository {

    override fun getAllDiagnosticCodes(): Flow<List<DiagnosticCode>> {
        return diagnosticCodeDao.getAllDiagnosticCodes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getBillableDiagnosticCodes(): Flow<List<DiagnosticCode>> {
        return diagnosticCodeDao.getBillableDiagnosticCodes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getCommonDiagnosticCodes(): Flow<List<DiagnosticCode>> {
        return diagnosticCodeDao.getCommonDiagnosticCodes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchDiagnosticCodes(query: String): Flow<List<DiagnosticCode>> {
        val wildcardQuery = "%$query%"
        return diagnosticCodeDao.searchDiagnosticCodes(wildcardQuery).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getDiagnosticCodeById(id: Long): DiagnosticCode? {
        return diagnosticCodeDao.getDiagnosticCodeById(id)?.toDomainModel()
    }

    override suspend fun getDiagnosticCodeByIcdCode(icdCode: String): DiagnosticCode? {
        return diagnosticCodeDao.getDiagnosticCodeByIcdCode(icdCode)?.toDomainModel()
    }

    override suspend fun getDiagnosticCodeCount(): Int {
        return diagnosticCodeDao.getDiagnosticCodeCount()
    }

    override suspend fun insertDiagnosticCode(code: DiagnosticCode): Long {
        val entity = DiagnosticCodeEntity.fromDomainModel(code)
        return diagnosticCodeDao.insertDiagnosticCode(entity)
    }

    override suspend fun insertDiagnosticCodes(codes: List<DiagnosticCode>): List<Long> {
        val entities = codes.map { DiagnosticCodeEntity.fromDomainModel(it) }
        return diagnosticCodeDao.insertDiagnosticCodes(entities)
    }

    override suspend fun updateDiagnosticCode(code: DiagnosticCode) {
        val entity = DiagnosticCodeEntity.fromDomainModel(code.copy(
            // Update timestamp
            updatedAt = System.currentTimeMillis()
        ))
        diagnosticCodeDao.updateDiagnosticCode(entity)
    }

    override suspend fun deleteDiagnosticCode(code: DiagnosticCode) {
        val entity = DiagnosticCodeEntity.fromDomainModel(code)
        diagnosticCodeDao.deleteDiagnosticCode(entity)
    }

    override suspend fun updateCommonCodeStatus(codeId: Long, isCommon: Boolean): Boolean {
        return try {
            diagnosticCodeDao.updateCommonCodeStatus(codeId, isCommon)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteAllDiagnosticCodes() {
        diagnosticCodeDao.deleteAllDiagnosticCodes()
    }
}
