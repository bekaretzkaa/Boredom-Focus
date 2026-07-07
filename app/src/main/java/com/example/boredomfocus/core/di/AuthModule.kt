package com.example.boredomfocus.core.di

import com.example.boredomfocus.data.repository.FirebaseAuthRepositoryImpl
import com.example.boredomfocus.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepositoryImpl
    ) : AuthRepository

}