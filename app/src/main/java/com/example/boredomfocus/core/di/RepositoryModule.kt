package com.example.boredomfocus.core.di

import com.example.boredomfocus.core.settings.data.repository.AppSettingsRepositoryImpl
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
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
}