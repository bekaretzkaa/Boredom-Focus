package com.example.boredomfocus.data.repository

import com.example.boredomfocus.domain.model.AuthUser
import com.example.boredomfocus.domain.repository.AuthRepository
import com.example.boredomfocus.feature.auth.AuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun signUp(
        email: String,
        password: String
    ): AuthUser {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val user = result?.user ?: throw AuthException("Не удалось создать пользователя")

            AuthUser(
                uid = user.uid,
                email = user.email
            )
        } catch (e: Exception) {
            throw e.toAuthException()
        }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): AuthUser {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            val user = result?.user ?: throw AuthException("Не удалось войти в аккаунт")

            AuthUser(
                uid = user.uid,
                email = user.email
            )
        } catch (e: Exception) {
            throw e.toAuthException()
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

    private fun Exception.toAuthException(): AuthException {
        return when(this) {
            is FirebaseAuthWeakPasswordException ->  AuthException("Пароль слишком слабый")

            is FirebaseAuthUserCollisionException -> AuthException("Пользователь с таким email уже существует")

            is FirebaseAuthInvalidCredentialsException -> AuthException("Неверный email или пароль")

            is FirebaseAuthInvalidUserException -> AuthException("Пользователь с таким email не найден")

            is FirebaseNetworkException -> AuthException("Проблема с интернет-соединением")

            is AuthException -> this

            else -> AuthException(message ?: "Неизвестная ошибка")
        }
    }
}