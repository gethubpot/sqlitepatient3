package com.example.sqlitepatient3.di

import com.example.sqlitepatient3.domain.repository.EventRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
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
}