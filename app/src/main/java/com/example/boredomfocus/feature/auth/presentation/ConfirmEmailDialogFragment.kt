package com.example.boredomfocus.feature.auth.presentation

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
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    private var lastStatus: AuthUiStatus? = null

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
        // Cancel animations to prevent callbacks after view destruction
        _binding?.apply {
            layoutStatus.animate().cancel()
            cardDialog.animate().cancel()
        }

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

        playEnterAnimation()
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
                    val binding = _binding ?: return@collect

                    binding.tvEmailDescription2.text = state.email

                    if (lastStatus != state.status) {
                        lastStatus = state.status

                        animateStatusChange {
                            renderStatus(state.status)
                        }
                    }

                    binding.loadingOverlay.isVisible =
                        state.status == AuthUiStatus.Loading
                }
            }
        }
    }


    private fun renderStatus(status: AuthUiStatus) {
        val binding = _binding ?: return
        val context = context ?: return

        when(status) {

            AuthUiStatus.EmailSent -> {
                binding.cardIcon.setCardBackgroundColor(
                    Color.parseColor("#0F1A0F")
                )

                binding.cardIcon.setStrokeColor(
                    ContextCompat.getColor(context, R.color.green_basic)
                )

                binding.ivBackgroundIcon.setImageResource(R.drawable.ic_email)

                binding.tvEmail.text = getString(R.string.auth_confirm_email_title)
                binding.tvEmailDescription.text = getString(R.string.auth_confirm_email_description)
            }

            AuthUiStatus.EmailNotVerified -> {
                binding.cardIcon.setCardBackgroundColor(
                    Color.parseColor("#1E1010")
                )

                binding.cardIcon.setStrokeColor(
                    ContextCompat.getColor(context, R.color.red_basic)
                )

                binding.ivBackgroundIcon.setImageResource(R.drawable.ic_email_not)

                binding.tvEmail.text = getString(R.string.auth_confirm_email_not_verified_title)
                binding.tvEmailDescription.text = getString(R.string.auth_confirm_email_not_verified_description)
            }

            else -> Unit
        }
    }

    private fun animateStatusChange(block: () -> Unit) {
        _binding?.layoutStatus?.animate()
            ?.alpha(0f)
            ?.translationY(-16f)
            ?.setDuration(120)
            ?.withEndAction {
                if (_binding == null) return@withEndAction

                block()

                _binding?.layoutStatus?.apply {
                    translationY = 16f

                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(220)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
            }
            ?.start()
    }

    private fun playEnterAnimation() {
        _binding?.cardDialog?.apply {
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
}