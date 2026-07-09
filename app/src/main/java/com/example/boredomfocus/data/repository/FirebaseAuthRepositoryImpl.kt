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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser?.toAuthUser())
    override fun getCurrentUser(): Flow<AuthUser?> = _currentUser.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser?.toAuthUser()
        }
    }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String
    ): AuthResult<AuthUser> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val user = result.user ?: return AuthResult.Error(AuthError.Unknown)

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()

            user.updateProfile(profileUpdates).await()

            val authUser = AuthUser(
                uid = user.uid,
                name = name,
                email = user.email ?: email
            )
            _currentUser.value = authUser
            AuthResult.Success(authUser)
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

            delay(1000)

            val authUser = AuthUser(
                uid = user.uid,
                name = user.displayName,
                email = user.email
            )

            _currentUser.value = authUser
            AuthResult.Success(authUser)
        } catch (e: Exception) {
            delay(2000)
            AuthResult.Error(e.toAuthError())
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
        _currentUser.value = null
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult<AuthUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val result = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val user = result.user ?: return AuthResult.Error(AuthError.Unknown)

            AuthResult.Success(user.toAuthUser())
        } catch (e: Exception) {
            AuthResult.Error(e.toAuthError())
        }
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

    private fun FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            name = displayName,
            email = email
        )
    }
}