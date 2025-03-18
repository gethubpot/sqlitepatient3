package com.example.sqlitepatient3.domain.usecase.facility

import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllFacilitiesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    operator fun invoke(): Flow<List<Facility>> {
        return facilityRepository.getAllFacilities()
    }
}

class GetActiveFacilitiesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    operator fun invoke(): Flow<List<Facility>> {
        return facilityRepository.getActiveFacilities()
    }
}

class GetFacilityByIdUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(id: Long): Facility? {
        return facilityRepository.getFacilityById(id)
    }
}

class SearchFacilitiesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    operator fun invoke(query: String): Flow<List<Facility>> {
        return facilityRepository.searchFacilities(query)
    }
}

class AddFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(facility: Facility): Long {
        return facilityRepository.insertFacility(facility)
    }
}

class UpdateFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(facility: Facility) {
        facilityRepository.updateFacility(facility)
    }
}

class DeleteFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(facility: Facility) {
        facilityRepository.deleteFacility(facility)
    }
}

class ToggleFacilityStatusUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(facilityId: Long): Boolean {
        return facilityRepository.toggleFacilityActiveStatus(facilityId)
    }
}

class GetFacilityWithPatientsUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    operator fun invoke(facilityId: Long): Flow<Pair<Facility, List<Patient>>?> {
        return facilityRepository.getFacilityWithPatients(facilityId)
    }
}