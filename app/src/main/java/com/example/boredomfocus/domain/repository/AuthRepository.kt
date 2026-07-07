package com.example.boredomfocus.domain.repository

import com.example.boredomfocus.domain.model.AuthUser

interface AuthRepository {

    suspend fun signUp(
        email: String,
        password: String
    ): AuthUser

    suspend fun signIn(
        email: String,
        password: String
    ): AuthUser

    fun getCurrentUser(): AuthUser?

    fun signOut()

}