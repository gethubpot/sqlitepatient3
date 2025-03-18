package com.example.sqlitepatient3.di

import com.example.sqlitepatient3.data.repository.EventRepositoryImpl
import com.example.sqlitepatient3.data.repository.FacilityRepositoryImpl
import com.example.sqlitepatient3.data.repository.PatientRepositoryImpl
import com.example.sqlitepatient3.domain.repository.EventRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPatientRepository(
        patientRepositoryImpl: PatientRepositoryImpl
    ): PatientRepository

    @Binds
    @Singleton
    abstract fun bindFacilityRepository(
        facilityRepositoryImpl: FacilityRepositoryImpl
    ): FacilityRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository
}