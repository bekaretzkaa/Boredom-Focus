package com.example.boredomfocus.feature.auth.presentation

import com.example.boredomfocus.domain.model.AuthUser
import com.example.boredomfocus.domain.repository.AuthRepository
import com.example.boredomfocus.rule.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AuthViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    private fun createViewModel(): AuthViewModel =
        AuthViewModel(authRepository)


    @Test
    fun `signUp returns EmptySignUp when empty`() {
        val viewModel = createViewModel()

        viewModel.signUp()
        val state = viewModel.uiState.value

        assertEquals(AuthUiStatus.EmptySignUp, state.status)
    }

    @Test
    fun `signUp return EmptyTwo when 2+ fields empty`() {
        val viewModel = createViewModel()
        viewModel.onNameChanged("Bekzat")
        viewModel.onEmailChanged("bekaretzka@gmail.com")

        viewModel.signUp()
        val state = viewModel.uiState.value

        assertEquals(AuthUiStatus.EmptyTwo(
            name = false,
            email = false,
            password = true,
            confirmPassword = true
        ), state.status)
    }

    @Test
    fun `signUp return EmptyPassword when password field is empty`() {
        val viewModel = createViewModel()
        viewModel.onNameChanged("Bekzat")
        viewModel.onEmailChanged("bekaretzka@gmail.com")
        viewModel.onConfirmPasswordChanged("aaa123")

        viewModel.signUp()
        val state = viewModel.uiState.value

        assertEquals(AuthUiStatus.EmptyPassword, state.status)
    }

    @Test
    fun `signUp return PasswordMismatch when passwords don't match`() {
        val viewModel = createViewModel()
        viewModel.onNameChanged("Bekzat")
        viewModel.onEmailChanged("bekaretzka@gmail.com")
        viewModel.onPasswordChanged("aaa123")
        viewModel.onConfirmPasswordChanged("bbb123")

        viewModel.signUp()
        val state = viewModel.uiState.value

        assertEquals(AuthUiStatus.PasswordMismatch, state.status)
    }

    @Test
    fun `signUp sets EmailSent when registration and verification succeed`() = runTest {
        coEvery {
            authRepository.signUp(
                "Bekzat",
                "absent@gmail.com",
                "aaa123"
            )
        } returns AuthResult.Success(AuthUser("001", "Bekzat", "absent@gmail.com"))

        coEvery {
            authRepository.sendEmailVerification()
        } returns AuthResult.Success(Unit)

        val viewModel = createViewModel()

        viewModel.onNameChanged("Bekzat")
        viewModel.onEmailChanged("absent@gmail.com")
        viewModel.onPasswordChanged("aaa123")
        viewModel.onConfirmPasswordChanged("aaa123")

        viewModel.signUp()

        advanceUntilIdle()

        coVerify {
            authRepository.signUp(
                "Bekzat",
                "absent@gmail.com",
                "aaa123"
            )
        }

        coVerify {
            authRepository.sendEmailVerification()
        }

        val state = viewModel.uiState.value

        assertEquals(AuthUiStatus.EmailSent, state.status)
    }



    @Test
    fun `singIn returns EmptySignIn when fields are empty`() {
        val viewModel = createViewModel()

        viewModel.signIn()
        val state = viewModel.uiState.value

        assertEquals(AuthUiStatus.EmptySignIn, state.status)
    }

    @Test
    fun `singIn returns Success when sign in is successful`() = runTest {
        coEvery {
            authRepository.signIn(
                "absent@gmail.com",
                "aaa123"
            )
        } returns AuthResult.Success(AuthUser("001", "Bekzat", "absent@gmail.com"))

        val viewModel = createViewModel()
        viewModel.onEmailChanged("absent@gmail.com")
        viewModel.onPasswordChanged("aaa123")

        viewModel.signIn()

        coVerify {
            authRepository.signIn(
                "absent@gmail.com",
                "aaa123"
            )
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(AuthUiStatus.Success, state.status)
    }

    @Test
    fun `singIn returns InvalidCredentials when sign in fails`() = runTest {
        coEvery {
            authRepository.signIn(
                "absent@gmail.com",
                "aaa123"
            )
        } returns AuthResult.Error(AuthError.InvalidCredentials)

        val viewModel = createViewModel()
        viewModel.onEmailChanged("absent@gmail.com")
        viewModel.onPasswordChanged("aaa123")

        viewModel.signIn()

        coVerify {
            authRepository.signIn(
                "absent@gmail.com",
                "aaa123"
            )
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthUiStatus.InvalidCredentials, state.status)
    }



    @Test
    fun `checkEmailVerification returns Success when verification is successful`() = runTest {
        coEvery {
            authRepository.checkEmailVerification()
        } returns AuthResult.Success(true)

        val viewModel = createViewModel()
        viewModel.checkEmailVerification()

        coVerify {
            authRepository.checkEmailVerification()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthUiStatus.Success, state.status)
        assertTrue(state.isEmailVerified)
    }

    @Test
    fun `checkEmailVerification returns EmailNotVerified when verification is not successful`() = runTest {
        coEvery {
            authRepository.checkEmailVerification()
        } returns AuthResult.Success(false)

        val viewModel = createViewModel()
        viewModel.checkEmailVerification()

        coVerify {
            authRepository.checkEmailVerification()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthUiStatus.EmailNotVerified, state.status)
        assertTrue(!state.isEmailVerified)
    }



    @Test
    fun `onConfirmEmailScreenClosed deletes account when not verified`() = runTest {
        coEvery {
            authRepository.checkEmailVerification()
        } returns AuthResult.Success(false)

        val viewModel = createViewModel()
        viewModel.checkEmailVerification()

        advanceUntilIdle()

        viewModel.onConfirmEmailScreenClosed()

        coVerify {
            authRepository.deleteAccount()
            authRepository.signOut()
        }
    }

    @Test
    fun `onConfirmEmailScreenClosed does not delete account when verified`() = runTest {
        coEvery {
            authRepository.checkEmailVerification()
        } returns AuthResult.Success(true)

        val viewModel = createViewModel()
        viewModel.checkEmailVerification()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmailVerified)

        viewModel.onConfirmEmailScreenClosed()

        advanceUntilIdle()

        coVerify(exactly = 0) {
            authRepository.deleteAccount()
            authRepository.signOut()
        }
    }
}