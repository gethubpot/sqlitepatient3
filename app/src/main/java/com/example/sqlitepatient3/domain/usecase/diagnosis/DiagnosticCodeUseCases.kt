package com.example.sqlitepatient3.domain.usecase.diagnosis

import com.example.sqlitepatient3.domain.model.DiagnosticCode
import com.example.sqlitepatient3.domain.repository.DiagnosticCodeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllDiagnosticCodesUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    operator fun invoke(): Flow<List<DiagnosticCode>> {
        return diagnosticCodeRepository.getAllDiagnosticCodes()
    }
}

class GetBillableDiagnosticCodesUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    operator fun invoke(): Flow<List<DiagnosticCode>> {
        return diagnosticCodeRepository.getBillableDiagnosticCodes()
    }
}

class GetCommonDiagnosticCodesUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    operator fun invoke(): Flow<List<DiagnosticCode>> {
        return diagnosticCodeRepository.getCommonDiagnosticCodes()
    }
}

class SearchDiagnosticCodesUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    operator fun invoke(query: String): Flow<List<DiagnosticCode>> {
        return diagnosticCodeRepository.searchDiagnosticCodes(query)
    }
}

class GetDiagnosticCodeByIdUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke(id: Long): DiagnosticCode? {
        return diagnosticCodeRepository.getDiagnosticCodeById(id)
    }
}

class GetDiagnosticCodeByIcdCodeUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke(icdCode: String): DiagnosticCode? {
        return diagnosticCodeRepository.getDiagnosticCodeByIcdCode(icdCode)
    }
}

class AddDiagnosticCodeUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke(
        icdCode: String,
        description: String,
        shorthand: String? = null,
        billable: Boolean = true,
        commonCode: Int? = null // Changed parameter type to Int? and default to null
    ): Long {
        val code = DiagnosticCode(
            icdCode = icdCode,
            description = description,
            shorthand = shorthand,
            billable = billable,
            commonCode = commonCode // Now assigning Int? to Int? (Correct)
        )
        return diagnosticCodeRepository.insertDiagnosticCode(code)
    }
}

class UpdateDiagnosticCodeUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke(code: DiagnosticCode) {
        diagnosticCodeRepository.updateDiagnosticCode(code)
    }
}

class DeleteDiagnosticCodeUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke(code: DiagnosticCode) {
        diagnosticCodeRepository.deleteDiagnosticCode(code)
    }
}

class UpdateCommonCodeStatusUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke(codeId: Long, isCommon: Boolean): Boolean {
        return diagnosticCodeRepository.updateCommonCodeStatus(codeId, isCommon)
    }
}

class BulkInsertDiagnosticCodesUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke(codes: List<DiagnosticCode>): List<Long> {
        return diagnosticCodeRepository.insertDiagnosticCodes(codes)
    }
}

class DeleteAllDiagnosticCodesUseCase @Inject constructor(
    private val diagnosticCodeRepository: DiagnosticCodeRepository
) {
    suspend operator fun invoke() {
        diagnosticCodeRepository.deleteAllDiagnosticCodes()
    }
}