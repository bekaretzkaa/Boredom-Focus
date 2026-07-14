package com.example.boredomfocus.domain.repository

import com.example.boredomfocus.domain.model.AuthUser
import com.example.boredomfocus.feature.auth.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun signUp(
        name: String,
        email: String,
        password: String
    ): AuthResult<AuthUser>

    suspend fun signIn(
        email: String,
        password: String
    ): AuthResult<AuthUser>

    fun getCurrentUser(): Flow<AuthUser?>

    fun signOut()

    suspend fun signInWithGoogle(idToken: String) : AuthResult<AuthUser>

    suspend fun sendEmailVerification() : AuthResult<Unit>

    suspend fun checkEmailVerification() : AuthResult<Boolean>

    suspend fun deleteAccount()

}