package com.example.sqlitepatient3.di

import com.example.sqlitepatient3.domain.repository.DiagnosticCodeRepository
import com.example.sqlitepatient3.domain.repository.EventRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientDiagnosisRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
import com.example.sqlitepatient3.domain.usecase.diagnosis.*
import com.example.sqlitepatient3.domain.usecase.event.*
import com.example.sqlitepatient3.domain.usecase.facility.*
import com.example.sqlitepatient3.domain.usecase.patient.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // Patient Use Cases
    @Provides
    @Singleton
    fun provideGetAllPatientsUseCase(repository: PatientRepository): GetAllPatientsUseCase {
        return GetAllPatientsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetPatientByIdUseCase(repository: PatientRepository): GetPatientByIdUseCase {
        return GetPatientByIdUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchPatientsUseCase(repository: PatientRepository): SearchPatientsUseCase {
        return SearchPatientsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddPatientUseCase(repository: PatientRepository): AddPatientUseCase {
        return AddPatientUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdatePatientUseCase(repository: PatientRepository): UpdatePatientUseCase {
        return UpdatePatientUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeletePatientUseCase(repository: PatientRepository): DeletePatientUseCase {
        return DeletePatientUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdatePatientStatusesUseCase(repository: PatientRepository): UpdatePatientStatusesUseCase {
        return UpdatePatientStatusesUseCase(repository)
    }

    // Facility Use Cases
    @Provides
    @Singleton
    fun provideGetAllFacilitiesUseCase(repository: FacilityRepository): GetAllFacilitiesUseCase {
        return GetAllFacilitiesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetActiveFacilitiesUseCase(repository: FacilityRepository): GetActiveFacilitiesUseCase {
        return GetActiveFacilitiesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetFacilityByIdUseCase(repository: FacilityRepository): GetFacilityByIdUseCase {
        return GetFacilityByIdUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchFacilitiesUseCase(repository: FacilityRepository): SearchFacilitiesUseCase {
        return SearchFacilitiesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddFacilityUseCase(repository: FacilityRepository): AddFacilityUseCase {
        return AddFacilityUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateFacilityUseCase(repository: FacilityRepository): UpdateFacilityUseCase {
        return UpdateFacilityUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteFacilityUseCase(repository: FacilityRepository): DeleteFacilityUseCase {
        return DeleteFacilityUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideToggleFacilityStatusUseCase(repository: FacilityRepository): ToggleFacilityStatusUseCase {
        return ToggleFacilityStatusUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetFacilityWithPatientsUseCase(repository: FacilityRepository): GetFacilityWithPatientsUseCase {
        return GetFacilityWithPatientsUseCase(repository)
    }

    // Event Use Cases
    @Provides
    @Singleton
    fun provideGetAllEventsUseCase(repository: EventRepository): GetAllEventsUseCase {
        return GetAllEventsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetEventsByPatientUseCase(repository: EventRepository): GetEventsByPatientUseCase {
        return GetEventsByPatientUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetEventsByTypeUseCase(repository: EventRepository): GetEventsByTypeUseCase {
        return GetEventsByTypeUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetEventsByStatusUseCase(repository: EventRepository): GetEventsByStatusUseCase {
        return GetEventsByStatusUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetEventsByDateRangeUseCase(repository: EventRepository): GetEventsByDateRangeUseCase {
        return GetEventsByDateRangeUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetRecentEventsUseCase(repository: EventRepository): GetRecentEventsUseCase {
        return GetRecentEventsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetUnbilledEventsUseCase(repository: EventRepository): GetUnbilledEventsUseCase {
        return GetUnbilledEventsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetEventByIdUseCase(repository: EventRepository): GetEventByIdUseCase {
        return GetEventByIdUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddEventUseCase(repository: EventRepository): AddEventUseCase {
        return AddEventUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateEventUseCase(repository: EventRepository): UpdateEventUseCase {
        return UpdateEventUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteEventUseCase(repository: EventRepository): DeleteEventUseCase {
        return DeleteEventUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateEventStatusUseCase(repository: EventRepository): UpdateEventStatusUseCase {
        return UpdateEventStatusUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteAllPatientEventsUseCase(repository: EventRepository): DeleteAllPatientEventsUseCase {
        return DeleteAllPatientEventsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetEventStatisticsUseCase(repository: EventRepository): GetEventStatisticsUseCase {
        return GetEventStatisticsUseCase(repository)
    }

    // DiagnosticCode Use Cases
    @Provides
    @Singleton
    fun provideGetAllDiagnosticCodesUseCase(repository: DiagnosticCodeRepository): GetAllDiagnosticCodesUseCase {
        return GetAllDiagnosticCodesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetBillableDiagnosticCodesUseCase(repository: DiagnosticCodeRepository): GetBillableDiagnosticCodesUseCase {
        return GetBillableDiagnosticCodesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetCommonDiagnosticCodesUseCase(repository: DiagnosticCodeRepository): GetCommonDiagnosticCodesUseCase {
        return GetCommonDiagnosticCodesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchDiagnosticCodesUseCase(repository: DiagnosticCodeRepository): SearchDiagnosticCodesUseCase {
        return SearchDiagnosticCodesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetDiagnosticCodeByIdUseCase(repository: DiagnosticCodeRepository): GetDiagnosticCodeByIdUseCase {
        return GetDiagnosticCodeByIdUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddDiagnosticCodeUseCase(repository: DiagnosticCodeRepository): AddDiagnosticCodeUseCase {
        return AddDiagnosticCodeUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateDiagnosticCodeUseCase(repository: DiagnosticCodeRepository): UpdateDiagnosticCodeUseCase {
        return UpdateDiagnosticCodeUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateCommonCodeStatusUseCase(repository: DiagnosticCodeRepository): UpdateCommonCodeStatusUseCase {
        return UpdateCommonCodeStatusUseCase(repository)
    }

    // PatientDiagnosis Use Cases
    @Provides
    @Singleton
    fun provideGetPatientDiagnosesUseCase(repository: PatientDiagnosisRepository): GetPatientDiagnosesUseCase {
        return GetPatientDiagnosesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetActivePatientDiagnosesUseCase(repository: PatientDiagnosisRepository): GetActivePatientDiagnosesUseCase {
        return GetActivePatientDiagnosesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetHospiceDiagnosesUseCase(repository: PatientDiagnosisRepository): GetHospiceDiagnosesUseCase {
        return GetHospiceDiagnosesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddPatientDiagnosisUseCase(repository: PatientDiagnosisRepository): AddPatientDiagnosisUseCase {
        return AddPatientDiagnosisUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdatePatientDiagnosisUseCase(repository: PatientDiagnosisRepository): UpdatePatientDiagnosisUseCase {
        return UpdatePatientDiagnosisUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSetDiagnosisActiveUseCase(repository: PatientDiagnosisRepository): SetDiagnosisActiveUseCase {
        return SetDiagnosisActiveUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSetHospiceStatusUseCase(repository: PatientDiagnosisRepository): SetHospiceStatusUseCase {
        return SetHospiceStatusUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideResolveDiagnosisUseCase(repository: PatientDiagnosisRepository): ResolveDiagnosisUseCase {
        return ResolveDiagnosisUseCase(repository)
    }
}