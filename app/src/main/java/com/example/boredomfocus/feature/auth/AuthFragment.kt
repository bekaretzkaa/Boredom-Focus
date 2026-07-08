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

                        binding.tvCreate.setOnClickListener {
                            viewModel.onSignTypeChanged(false)
                        }

                        binding.btnSignIn.setOnClickListener {
                            viewModel.signIn()
                        }
                    } else {
                        binding.tvAuth.text = "Создать аккаунт"
                        binding.tvPasswordConfirm.visibility = View.VISIBLE
                        binding.tilPasswordConfirm.visibility = View.VISIBLE
                        binding.tvPasswordForgot.visibility = View.GONE
                        binding.btnSignIn.text = "Создать аккаунт"
                        binding.tvNoAccount.text = "Уже есть аккаунт?"
                        binding.tvCreate.setText(R.string.sign_in_account)

                        binding.tvCreate.setOnClickListener {
                            viewModel.onSignTypeChanged(true)
                        }

                        binding.btnSignIn.setOnClickListener {
                            viewModel.signUp()
                        }
                    }

                    val redBasic = ContextCompat.getColor(requireContext(), R.color.red_basic)
                    val textFieldIcon = ContextCompat.getColor(requireContext(), R.color.text_field_icon)

                    binding.tilPassword.error = null
                    binding.tilPassword.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
                    binding.tvPasswordWrong.visibility = View.GONE

                    binding.tilEmail.error = null
                    binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(textFieldIcon))
                    binding.tvEmailWrong.visibility = View.GONE

                    binding.cardToast.visibility = View.GONE

                    when(state.status) {
                        is AuthUiStatus.Success -> {
                            binding.cardToast.visibility = View.VISIBLE
                            binding.cardToast.strokeColor = ContextCompat.getColor(requireContext(), R.color.green_basic)
                            binding.llToast.background  = ContextCompat.getColor(requireContext(), R.color.green_bg).toDrawable()
                            binding.ivToast.setBackgroundResource(R.drawable.ic_check_green)
                            binding.tvToast.text = if(state.isSignIn) "Успешный вход" else "Успешная регистрация"
                            binding.tvToast.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_basic))

                            delay(5000)

                            findNavController().popBackStack()
                        }
                        is AuthUiStatus.WeakPassword -> {
                            binding.tilPassword.error = redBasic.toString()
                            binding.tilPassword.setEndIconTintList(ColorStateList.valueOf(redBasic))
                            binding.tvPasswordWrong.visibility = View.VISIBLE
                            binding.tvPasswordWrong.text = "Пароль слишком слабый"
                        }
                        is AuthUiStatus.EmailAlreadyExists -> {
                            binding.tilEmail.error = redBasic.toString()
                            binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(redBasic))
                            binding.tvEmailWrong.visibility = View.VISIBLE
                            binding.tvEmailWrong.text = "Пользователь с таким email уже существует"
                        }
                        is AuthUiStatus.InvalidCredentials -> {
                            binding.tilEmail.error = redBasic.toString()
                            binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(redBasic))

                            binding.tilPassword.error = redBasic.toString()
                            binding.tilPassword.setEndIconTintList(ColorStateList.valueOf(redBasic))
                            binding.tvPasswordWrong.visibility = View.VISIBLE
                            binding.tvPasswordWrong.text = "Неверный email или пароль"
                        }
                        is AuthUiStatus.UserNotFound -> {
                            binding.tilEmail.error = redBasic.toString()
                            binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(redBasic))
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
                            binding.tilEmail.error = redBasic.toString()
                            binding.tilEmail.setEndIconTintList(ColorStateList.valueOf(redBasic))

                            binding.tilPassword.error = redBasic.toString()
                            binding.tilPassword.setEndIconTintList(ColorStateList.valueOf(redBasic))
                            binding.tvPasswordWrong.visibility = View.VISIBLE
                            binding.tvPasswordWrong.text = "Незвестная ошибка"
                        }

                        is AuthUiStatus.Loading -> {

                        }

                        is AuthUiStatus.Idle -> {

                        }
                    }
                }
            }
        }
    }
}