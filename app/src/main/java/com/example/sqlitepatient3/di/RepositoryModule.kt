package com.example.sqlitepatient3.di

import com.example.sqlitepatient3.data.repository.DiagnosticCodeRepositoryImpl
import com.example.sqlitepatient3.data.repository.EventRepositoryImpl
import com.example.sqlitepatient3.data.repository.FacilityRepositoryImpl
import com.example.sqlitepatient3.data.repository.PatientDiagnosisRepositoryImpl
import com.example.sqlitepatient3.data.repository.PatientRepositoryImpl
import com.example.sqlitepatient3.domain.repository.DiagnosticCodeRepository
import com.example.sqlitepatient3.domain.repository.EventRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientDiagnosisRepository
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

    @Binds
    @Singleton
    abstract fun bindDiagnosticCodeRepository(
        diagnosticCodeRepositoryImpl: DiagnosticCodeRepositoryImpl
    ): DiagnosticCodeRepository

    @Binds
    @Singleton
    abstract fun bindPatientDiagnosisRepository(
        patientDiagnosisRepositoryImpl: PatientDiagnosisRepositoryImpl
    ): PatientDiagnosisRepository
}