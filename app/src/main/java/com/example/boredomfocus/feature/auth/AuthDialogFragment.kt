package com.example.boredomfocus.feature.auth

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.DialogAuthBinding
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthDialogFragment : DialogFragment() {

    private val viewModel: AuthViewModel by hiltNavGraphViewModels(R.id.authGraph)
    private var _binding: DialogAuthBinding? = null
    private val binding get() = _binding!!
    private lateinit var credentialManager: CredentialManager

    private var lastSignInMode: Boolean? = null
    private var lastStatus: AuthUiStatus? = null
    private var toastJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.CustomCenterDialogTheme)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val params = attributes
            params.dimAmount = 0.78f
            attributes = params

            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        credentialManager = CredentialManager.create(requireContext())

        binding.cardGoogle.setOnClickListener {
            signInWithGoogle()
        }

        observeEvents()

        binding.etEmail.doAfterTextChanged {
            viewModel.onEmailChanged(it.toString())
        }
        binding.etPassword.doAfterTextChanged {
            viewModel.onPasswordChanged(it.toString())
        }
        binding.etName.doAfterTextChanged {
            viewModel.onNameChanged(it.toString())
        }
        binding.etPasswordConfirm.doAfterTextChanged {
            viewModel.onConfirmPasswordChanged(it.toString())
        }

        binding.flExit.setOnClickListener {
            dismissAnimated()
        }

        binding.tvCreate.setOnClickListener {
            Log.d("AUTH", "CLICK: ${viewModel.uiState.value.isSignIn}")
            viewModel.onSignTypeChanged(!viewModel.uiState.value.isSignIn)
        }

        binding.btnSignIn.setOnClickListener {
            if (viewModel.uiState.value.isSignIn) {
                viewModel.signIn()
            } else {
                viewModel.signUp()
            }
        }

        observeUiState()
        playEnterAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        toastJob?.cancel()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when(event) {
                        AuthUiEvent.LoginCompleted -> {
                            findNavController().popBackStack(R.id.settingsFragment, false)
                        }

                        AuthUiEvent.OpenConfirmEmail -> {
                            findNavController().navigate(R.id.actionAuthFragmentToConfirmEmailFragment)
                        }

                        AuthUiEvent.RegistrationCompleted -> {

                        }
                    }
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    if(lastSignInMode != state.isSignIn) {
                        lastSignInMode = state.isSignIn

                        animateFormChange {
                            binding.tvPasswordConfirm.isVisible = !state.isSignIn
                            binding.tilPasswordConfirm.isVisible = !state.isSignIn
                            binding.tvPasswordForgot.isVisible = state.isSignIn
                            binding.tilName.isVisible = !state.isSignIn
                            binding.tvName.isVisible = !state.isSignIn
                        }

                        binding.tvAuth.animate()
                            .alpha(0f)
                            .setDuration(80)
                            .withEndAction {
                                binding.tvAuth.text =
                                    if(state.isSignIn) "Войти" else "Создать аккаунт"

                                binding.tvAuth.animate()
                                    .alpha(1f)
                                    .setDuration(120)
                                    .start()
                            }.start()

                        animateButton(if(state.isSignIn) "Войти" else "Создать аккаунт")

                    }

                    if(lastStatus != state.status) {
                        lastStatus = state.status

                        renderStatus(state)
                    }

                    if(state.isSignIn) {
                        binding.tvNoAccount.text = "Нет аккаунта?"
                        binding.tvCreate.setText(R.string.create_account)

                        binding.etName.text?.clear()
                        binding.etPasswordConfirm.text?.clear()

                        binding.tvSigning.text = "Входим..."
                    } else {
                        binding.tvNoAccount.text = "Уже есть аккаунт?"
                        binding.tvCreate.setText(R.string.sign_in_account)
                        binding.tvSigning.text = "Регистрируемся..."
                    }
                }
            }
        }
    }

    private fun renderStatus(state: AuthUiState) {
        clearStatus()

        val redBasic = ContextCompat.getColor(requireContext(), R.color.red_basic)

        when(state.status) {
            is AuthUiStatus.Success -> {
                findNavController().popBackStack()
            }
            is AuthUiStatus.WeakPassword -> {
                makeRed(redBasic,
                    name = false,
                    email = false,
                    password = true,
                    confirmPassword = false
                )
                showError(binding.tvPasswordWrong)
                binding.tvPasswordWrong.text = "Пароль слишком слабый"
            }
            is AuthUiStatus.EmailAlreadyExists -> {
                makeRed(redBasic, name = false, email = true, password = false, confirmPassword = false)
                showError(binding.tvEmailWrong)
                binding.tvEmailWrong.text = "Пользователь с таким email уже существует"
            }
            is AuthUiStatus.InvalidCredentials -> {
                makeRed(redBasic, name = false, email = true, password = true, confirmPassword = false)
                showError(binding.tvPasswordWrong)
                binding.tvPasswordWrong.text = "Неверный email или пароль"
            }
            is AuthUiStatus.UserNotFound -> {
                makeRed(redBasic, name = false, email = true, password = false, confirmPassword = false)
                showError(binding.tvEmailWrong)
                binding.tvEmailWrong.text = "Пользователь с таким email не найден"
            }
            is AuthUiStatus.NetworkError -> {
                showToast()
                binding.cardToast.strokeColor = ContextCompat.getColor(requireContext(), R.color.difficulty_orange)
                binding.llToast.background  = ContextCompat.getColor(requireContext(), R.color.difficulty_orange_bg_2).toDrawable()
                binding.ivToast.setBackgroundResource(R.drawable.ic_wifi_off)
                binding.tvToast.text = "Проблема с интернет-соединением"
                binding.tvToast.setTextColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange))
            }
            is AuthUiStatus.Unknown, AuthUiStatus.GoogleFailed -> {
                showToast()
                binding.cardToast.strokeColor = ContextCompat.getColor(requireContext(), R.color.red_basic)
                binding.llToast.background  = ContextCompat.getColor(requireContext(), R.color.difficulty_red_bg_2).toDrawable()
                binding.ivToast.setBackgroundResource(R.drawable.ic_warning_red)
                binding.tvToast.text = if(state.status == AuthUiStatus.GoogleFailed) "Не удалось авторизоваться с помощью Google" else "Неизвестная ошибка"
                binding.tvToast.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_basic))
            }

            is AuthUiStatus.EmptySignUp -> {
                makeRed(redBasic, name = true, email = true, password = true, confirmPassword = true)
                showError(binding.tvPasswordConfirmWrong)
                binding.tvPasswordConfirmWrong.text = "Заполните все поля"
            }

            is AuthUiStatus.EmptySignIn -> {
                makeRed(redBasic, name = false, email = true, password = true, confirmPassword = false)
                showError(binding.tvPasswordWrong)
                binding.tvPasswordWrong.text = "Заполните все поля"
            }

            is AuthUiStatus.EmptyName -> {
                makeRed(redBasic, name = true, email = false, password = false, confirmPassword = false)
                showError(binding.tvNameWrong)
                binding.tvNameWrong.text = "Заполните поле"
            }

            is AuthUiStatus.EmptyEmail -> {
                makeRed(redBasic, name = false, email = true, password = false, confirmPassword = false)
                showError(binding.tvEmailWrong)
                binding.tvEmailWrong.text = "Заполните поле"
            }

            is AuthUiStatus.EmptyPassword -> {
                makeRed(redBasic, name = false, email = false, password = true, confirmPassword = false)
                showError(binding.tvPasswordWrong)
                binding.tvPasswordWrong.text = "Заполните поле"
            }

            is AuthUiStatus.EmptyConfirmPassword -> {
                makeRed(redBasic, name = false, email = false, password = false, confirmPassword = true)
                showError(binding.tvPasswordConfirmWrong)
                binding.tvPasswordConfirmWrong.text = "Заполните поле"
            }

            is AuthUiStatus.PasswordMismatch -> {
                makeRed(redBasic, name = false, email = false, password = true, confirmPassword = true)
                showError(binding.tvPasswordConfirmWrong)
                binding.tvPasswordConfirmWrong.text = "Пароли не совпадают"
            }

            is AuthUiStatus.EmptyTwo -> {
                makeRed(redBasic, state.status.name, state.status.email, state.status.password, state.status.confirmPassword)
                showError(binding.tvPasswordConfirmWrong)
                binding.tvPasswordConfirmWrong.text = "Заполните все поля"
            }

            else -> {}
        }
    }

    private fun clearStatus() {

        val textFieldIcon = ContextCompat.getColor(requireContext(), R.color.text_field_icon)

        binding.tilName.error = null
        binding.tilName.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
        binding.tvNameWrong.isVisible = false

        binding.tilEmail.error = null
        binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
        binding.tvEmailWrong.isVisible = false

        binding.tilPassword.error = null
        binding.tilPassword.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
        binding.tvPasswordWrong.isVisible = false

        binding.tilPasswordConfirm.error = null
        binding.tilPasswordConfirm.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
        binding.tvPasswordConfirmWrong.isVisible = false
    }

    private fun makeRed(color: Int, name: Boolean ,email: Boolean, password: Boolean, confirmPassword: Boolean) {
        if(name) {
            binding.tilName.error = color.toString()
            binding.tilName.setEndIconTintList(ColorStateList.valueOf(color))
        }
        if (email) {
            binding.tilEmail.error = color.toString()
            binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(color))
        }
        if (password) {
            binding.tilPassword.error = color.toString()
            binding.tilPassword.setEndIconTintList(ColorStateList.valueOf(color))
        }
        if (confirmPassword) {
            binding.tilPasswordConfirm.error = color.toString()
            binding.tilPasswordConfirm.setEndIconTintList(ColorStateList.valueOf(color))
        }
    }

    private fun signInWithGoogle() {
        val googleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = requireActivity()
                )

                handleGoogleCredential(result)
            } catch (e: Exception) {
                viewModel.onGoogleSignInFailed()
            }
        }
    }

    private fun handleGoogleCredential(result: GetCredentialResponse) {
        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credential.data)

            viewModel.signInWithGoogle(
                idToken = googleIdTokenCredential.idToken
            )
        } else {
            viewModel.onGoogleSignInFailed()
        }
    }

    private fun playEnterAnimation() {
        binding.cardDialog.apply {
            alpha = 0f
            scaleX = 0.94f
            scaleY = 0.94f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }
    private fun dismissAnimated() {
        binding.cardDialog.animate()
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(170)
            .withEndAction {
                findNavController().popBackStack()
            }
            .start()
    }
    private fun animateFormChange(block: () -> Unit) {
        val transition = TransitionSet()
            .addTransition(
                ChangeBounds().apply {
                    duration = 280
                    interpolator = FastOutSlowInInterpolator()
                }
            )

        TransitionManager.beginDelayedTransition(
            binding.cardDialog,
            transition
        )

        block()
    }
    private fun animateButton(text: String) {
        binding.btnSignIn.animate()
            .alpha(0f)
            .setDuration(80)
            .withEndAction {
                binding.btnSignIn.text = text

                binding.btnSignIn.animate()
                    .alpha(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }
    private fun showError(view: View) {
        if(view.isVisible) return

        view.alpha = 0f
        view.translationY = -10f
        view.isVisible = true

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180)
            .start()
    }
    private fun showToast() {
        toastJob?.cancel()

        if (!binding.cardToast.isVisible) {
            binding.cardToast.apply {
                alpha = 0f
                translationY = -24f
                isVisible = true

                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }
        }

        toastJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(5000)
            hideToast()
            viewModel.clearStatus()
        }
    }
    private fun hideToast() {
        if (!binding.cardToast.isVisible) return

        binding.cardToast.animate()
            .alpha(0f)
            .translationY(-24f)
            .setDuration(180)
            .withEndAction {
                binding.cardToast.isVisible = false
                binding.cardToast.alpha = 1f
                binding.cardToast.translationY = 0f
            }
            .start()
    }
}