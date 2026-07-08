package com.example.boredomfocus.data.repository

import com.example.boredomfocus.domain.model.AuthUser
import com.example.boredomfocus.domain.repository.AuthRepository
import com.example.boredomfocus.feature.auth.AuthError
import com.example.boredomfocus.feature.auth.AuthResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun signUp(
        email: String,
        password: String
    ): AuthResult<AuthUser> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val user = result?.user ?: return AuthResult.Error(
                AuthError.Unknown
            )

            AuthResult.Success(
                AuthUser(
                    uid = user.uid,
                    email = user.email
                )
            )
        } catch (e: Exception) {
            AuthResult.Error(e.toAuthError())
        }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): AuthResult<AuthUser> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            val user = result?.user ?: return AuthResult.Error(
                AuthError.Unknown
            )

            delay(2000)
            AuthResult.Success(
                AuthUser(
                    uid = user.uid,
                    email = user.email
                )
            )
        } catch (e: Exception) {
            delay(2000)
            AuthResult.Error(e.toAuthError())
        }
    }

    override fun getCurrentUser(): AuthUser? {
        val user = firebaseAuth.currentUser

        return user?.let {
            AuthUser(
                uid = it.uid,
                email = it.email
            )
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private fun Exception.toAuthError(): AuthError {
        return when(this) {
            is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
            is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
            is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyExists
            is FirebaseAuthInvalidUserException -> AuthError.UserNotFound
            is FirebaseNetworkException -> AuthError.NetworkError
            else -> AuthError.Unknown
        }
    }
}