package com.example.sqlitepatient3.di

import android.content.Context
import com.example.sqlitepatient3.data.local.dao.EventDao
import com.example.sqlitepatient3.data.local.dao.FacilityDao
import com.example.sqlitepatient3.data.local.dao.PatientDao
import com.example.sqlitepatient3.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun providePatientDao(database: AppDatabase): PatientDao {
        return database.patientDao()
    }

    @Provides
    @Singleton
    fun provideFacilityDao(database: AppDatabase): FacilityDao {
        return database.facilityDao()
    }

    @Provides
    @Singleton
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }
}