package com.example.boredomfocus.core.di

import com.example.boredomfocus.core.appconfig.data.repository.AppSettingsRepositoryImpl
import com.example.boredomfocus.core.appconfig.domain.repository.AppSettingsRepository
import com.example.boredomfocus.data.repository.AddSessionRepositoryImpl
import com.example.boredomfocus.data.repository.DailyStatsRepositoryImpl
import com.example.boredomfocus.data.repository.SessionRepositoryImpl
import com.example.boredomfocus.domain.repository.AddSessionRepository
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
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
    abstract fun bindAppSettingsRepository(
        impl: AppSettingsRepositoryImpl
    ) : AppSettingsRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ) : SessionRepository

    @Binds
    @Singleton
    abstract fun bindDailyStatsRepository(
        impl: DailyStatsRepositoryImpl
    ) : DailyStatsRepository

    @Binds
    @Singleton
    abstract fun bindAddSessionRepository(
        impl: AddSessionRepositoryImpl
    ) : AddSessionRepository

}