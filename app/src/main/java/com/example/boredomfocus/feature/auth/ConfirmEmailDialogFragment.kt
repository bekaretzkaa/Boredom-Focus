package com.example.boredomfocus.feature.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.DialogConfirmEmailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfirmEmailDialogFragment : DialogFragment() {

    private val viewModel: AuthViewModel by hiltNavGraphViewModels(R.id.authGraph)
    private var _binding: DialogConfirmEmailBinding? = null
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
        _binding = DialogConfirmEmailBinding.inflate(inflater, container, false)
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

    override fun onDestroyView() {
        viewModel.onConfirmEmailScreenClosed()
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirmed.setOnClickListener {
            viewModel.checkEmailVerification()
        }

        binding.btnChangeEmail.setOnClickListener {
            viewModel.onConfirmEmailScreenClosed()
            findNavController().popBackStack()
        }

        observeUiState()
        observeEvents()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when(event) {
                        AuthUiEvent.RegistrationCompleted -> {
                            findNavController().popBackStack(R.id.settingsFragment, false)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingOverlay.isVisible = false
                    binding.tvEmailDescription2.text = state.email

                    when(state.status) {
                        AuthUiStatus.EmailSent -> {
                            binding.cardIcon.setBackgroundColor(Color.parseColor("#0F1A0F"))
                            binding.cardIcon.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.green_basic))
                            binding.ivBackgroundIcon.setImageResource(R.drawable.ic_email)

                            binding.tvEmail.text = "Подтвердите email"
                            binding.tvEmailDescription.text = "Мы отправили письмо на"
                        }

                        AuthUiStatus.EmailNotVerified -> {
                            binding.cardIcon.setBackgroundColor(Color.parseColor("#1E1010"))
                            binding.cardIcon.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.red_basic))
                            binding.ivBackgroundIcon.setImageResource(R.drawable.ic_email_not)

                            binding.tvEmail.text = "Email не подтвержден"
                            binding.tvEmailDescription.text = "Пройдите по ссылке из письма, затем нажмите кнопку снова."
                        }

                        AuthUiStatus.Loading -> {
                            binding.loadingOverlay.isVisible = true
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}