package com.example.boredomfocus.feature.auth

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.DialogAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthFragment : DialogFragment() {

    private val viewModel: AuthViewModel by viewModels()
    private var _binding: DialogAuthBinding? = null
    private val binding get() = _binding!!

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

        observeEvents()

        binding.etEmail.doAfterTextChanged {
            viewModel.onEmailChanged(it.toString())
        }
        binding.etPassword.doAfterTextChanged {
            viewModel.onPasswordChanged(it.toString())
        }

        binding.flExit.setOnClickListener {
            findNavController().popBackStack()
        }

        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when(event) {
                        is AuthUiEvent.ShowMessage -> {
                        }
                        is AuthUiEvent.NavigateToBack -> {
                            findNavController().popBackStack()
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
                    if(state.isSignIn) {
                        binding.tvAuth.text = "Войти"
                        binding.tvPasswordConfirm.visibility = View.GONE
                        binding.tilPasswordConfirm.visibility = View.GONE
                        binding.tvPasswordForgot.visibility = View.VISIBLE
                        binding.btnSignIn.text = "Войти"
                        binding.tvNoAccount.text = "Нет аккаунта?"
                        binding.tvCreate.setText(R.string.create_account)
                        binding.tilName.visibility = View.GONE
                        binding.tvName.visibility = View.GONE

                        binding.tvCreate.setOnClickListener {
                            viewModel.onSignTypeChanged(false)
                        }

                        binding.btnSignIn.setOnClickListener {
                            viewModel.signIn()
                        }

                        binding.etName.text?.clear()
                        binding.etPasswordConfirm.text?.clear()

                        binding.tvSigning.text = "Входим..."
                    } else {
                        binding.tvAuth.text = "Создать аккаунт"
                        binding.tvPasswordConfirm.visibility = View.VISIBLE
                        binding.tilPasswordConfirm.visibility = View.VISIBLE
                        binding.tvPasswordForgot.visibility = View.GONE
                        binding.btnSignIn.text = "Создать аккаунт"
                        binding.tvNoAccount.text = "Уже есть аккаунт?"
                        binding.tvCreate.setText(R.string.sign_in_account)
                        binding.tilName.visibility = View.VISIBLE
                        binding.tvName.visibility = View.VISIBLE

                        binding.tvCreate.setOnClickListener {
                            viewModel.onSignTypeChanged(true)
                        }

                        binding.btnSignIn.setOnClickListener {
                            viewModel.signUp()
                        }

                        binding.etPasswordConfirm.doAfterTextChanged {
                            viewModel.onConfirmPasswordChanged(it.toString())
                        }
                        binding.etName.doAfterTextChanged {
                            viewModel.onNameChanged(it.toString())
                        }
                        binding.tvSigning.text = "Регистрируемся..."
                    }

                    val redBasic = ContextCompat.getColor(requireContext(), R.color.red_basic)
                    val textFieldIcon = ContextCompat.getColor(requireContext(), R.color.text_field_icon)

                    binding.tilName.error = null
                    binding.tilName.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
                    binding.tvNameWrong.visibility = View.GONE

                    binding.tilEmail.error = null
                    binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
                    binding.tvEmailWrong.visibility = View.GONE

                    binding.tilPassword.error = null
                    binding.tilPassword.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
                    binding.tvPasswordWrong.visibility = View.GONE

                    binding.tilPasswordConfirm.error = null
                    binding.tilPasswordConfirm.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
                    binding.tvPasswordConfirmWrong.visibility = View.GONE

                    binding.cardToast.visibility = View.GONE

                    binding.loadingOverlay.visibility = if(state.status == AuthUiStatus.Loading) View.VISIBLE else View.GONE

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
                            binding.tvPasswordWrong.visibility = View.VISIBLE
                            binding.tvPasswordWrong.text = "Пароль слишком слабый"
                        }
                        is AuthUiStatus.EmailAlreadyExists -> {
                            makeRed(redBasic, name = false, email = true, password = false, confirmPassword = false)
                            binding.tvEmailWrong.visibility = View.VISIBLE
                            binding.tvEmailWrong.text = "Пользователь с таким email уже существует"
                        }
                        is AuthUiStatus.InvalidCredentials -> {
                            makeRed(redBasic, name = false, email = true, password = true, confirmPassword = false)
                            binding.tvPasswordWrong.visibility = View.VISIBLE
                            binding.tvPasswordWrong.text = "Неверный email или пароль"
                        }
                        is AuthUiStatus.UserNotFound -> {
                            makeRed(redBasic, name = false, email = true, password = false, confirmPassword = false)
                            binding.tvEmailWrong.visibility = View.VISIBLE
                            binding.tvEmailWrong.text = "Пользователь с таким email не найден"
                        }
                        is AuthUiStatus.NetworkError -> {
                            binding.cardToast.visibility = View.VISIBLE
                            binding.cardToast.strokeColor = ContextCompat.getColor(requireContext(), R.color.difficulty_orange)
                            binding.llToast.background  = ContextCompat.getColor(requireContext(), R.color.difficulty_orange_bg_2).toDrawable()
                            binding.ivToast.setBackgroundResource(R.drawable.ic_wifi_off)
                            binding.tvToast.text = "Проблема с интернет-соединением"
                            binding.tvToast.setTextColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange))
                        }
                        is AuthUiStatus.Unknown -> {
                            binding.cardToast.visibility = View.VISIBLE
                            binding.cardToast.strokeColor = ContextCompat.getColor(requireContext(), R.color.red_basic)
                            binding.llToast.background  = ContextCompat.getColor(requireContext(), R.color.difficulty_red_bg_2).toDrawable()
                            binding.ivToast.setBackgroundResource(R.drawable.ic_warning_red)
                            binding.tvToast.text = "Неизвестная ошибка"
                            binding.tvToast.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_basic))
                        }

                        is AuthUiStatus.EmptySignUp -> {
                            makeRed(redBasic, name = true, email = true, password = true, confirmPassword = true)
                            binding.tvPasswordConfirmWrong.visibility = View.VISIBLE
                            binding.tvPasswordConfirmWrong.text = "Заполните все поля"
                        }

                        is AuthUiStatus.EmptySignIn -> {
                            makeRed(redBasic, name = false, email = true, password = true, confirmPassword = false)
                            binding.tvPasswordWrong.visibility = View.VISIBLE
                            binding.tvPasswordWrong.text = "Заполните все поля"
                        }

                        is AuthUiStatus.EmptyName -> {
                            makeRed(redBasic, name = true, email = false, password = false, confirmPassword = false)
                            binding.tvNameWrong.visibility = View.VISIBLE
                            binding.tvNameWrong.text = "Заполните поле"
                        }

                        is AuthUiStatus.EmptyEmail -> {
                            makeRed(redBasic, name = false, email = true, password = false, confirmPassword = false)
                            binding.tvEmailWrong.visibility = View.VISIBLE
                            binding.tvEmailWrong.text = "Заполните поле"
                        }

                        is AuthUiStatus.EmptyPassword -> {
                            makeRed(redBasic, name = false, email = false, password = true, confirmPassword = false)
                            binding.tvPasswordWrong.visibility = View.VISIBLE
                            binding.tvPasswordWrong.text = "Заполните поле"
                        }

                        is AuthUiStatus.EmptyConfirmPassword -> {
                            makeRed(redBasic, name = false, email = false, password = false, confirmPassword = true)
                            binding.tvPasswordConfirmWrong.visibility = View.VISIBLE
                            binding.tvPasswordConfirmWrong.text = "Заполните поле"
                        }

                        is AuthUiStatus.PasswordMismatch -> {
                            makeRed(redBasic, name = false, email = false, password = true, confirmPassword = true)
                            binding.tvPasswordConfirmWrong.visibility = View.VISIBLE
                            binding.tvPasswordConfirmWrong.text = "Пароли не совпадают"
                        }

                        is AuthUiStatus.EmptyTwo -> {
                            makeRed(redBasic, state.status.name, state.status.email, state.status.password, state.status.confirmPassword)
                            binding.tvPasswordConfirmWrong.visibility = View.VISIBLE
                            binding.tvPasswordConfirmWrong.text = "Заполните все поля"
                        }

                        else -> {}
                    }
                }
            }
        }
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
}