package com.example.boredomfocus.domain.repository

import com.example.boredomfocus.domain.model.AuthUser
import com.example.boredomfocus.feature.auth.AuthResult

interface AuthRepository {

    suspend fun signUp(
        email: String,
        password: String
    ): AuthResult<AuthUser>

    suspend fun signIn(
        email: String,
        password: String
    ): AuthResult<AuthUser>

    fun getCurrentUser(): AuthUser?

    fun signOut()

}