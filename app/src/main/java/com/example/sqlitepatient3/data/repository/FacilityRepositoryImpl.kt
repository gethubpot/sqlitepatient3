package com.example.sqlitepatient3.data.repository

import com.example.sqlitepatient3.data.local.dao.FacilityDao
import com.example.sqlitepatient3.data.local.entity.FacilityEntity
import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacilityRepositoryImpl @Inject constructor(
    private val facilityDao: FacilityDao
) : FacilityRepository {

    override fun getAllFacilities(): Flow<List<Facility>> {
        return facilityDao.getAllFacilities().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getActiveFacilities(): Flow<List<Facility>> {
        return facilityDao.getActiveFacilities().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchFacilities(query: String): Flow<List<Facility>> {
        val wildcardQuery = "%$query%"
        return facilityDao.searchFacilities(wildcardQuery).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getFacilityById(id: Long): Facility? {
        return facilityDao.getFacilityById(id)?.toDomainModel()
    }

    override suspend fun getFacilityCount(): Int {
        return facilityDao.getFacilityCount()
    }

    override suspend fun getActiveFacilityCount(): Int {
        return facilityDao.getActiveFacilityCount()
    }

    override suspend fun insertFacility(facility: Facility): Long {
        val entity = FacilityEntity.Companion.fromDomainModel(facility)
        return facilityDao.insertFacility(entity)
    }

    override suspend fun insertFacilities(facilities: List<Facility>): List<Long> {
        val entities = facilities.map { FacilityEntity.Companion.fromDomainModel(it) }
        return facilityDao.insertFacilities(entities)
    }

    override suspend fun updateFacility(facility: Facility) {
        val entity = FacilityEntity.Companion.fromDomainModel(facility.copy(updatedAt = System.currentTimeMillis()))
        facilityDao.updateFacility(entity)
    }

    override suspend fun deleteFacility(facility: Facility) {
        val entity = FacilityEntity.Companion.fromDomainModel(facility)
        facilityDao.deleteFacility(entity)
    }

    override suspend fun toggleFacilityActiveStatus(facilityId: Long): Boolean {
        val facility = facilityDao.getFacilityById(facilityId) ?: return false
        facilityDao.updateActiveStatus(facilityId, !facility.isActive)
        return true
    }

    override fun getFacilityWithPatients(facilityId: Long): Flow<Pair<Facility, List<Patient>>?> {
        return facilityDao.getFacilityWithPatients(facilityId).mapNotNull { it?.toDomainModel() }
    }
}