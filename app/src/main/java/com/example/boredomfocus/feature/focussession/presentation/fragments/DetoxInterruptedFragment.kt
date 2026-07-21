package com.example.boredomfocus.feature.focussession.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentDetoxInterruptedBinding
import com.example.boredomfocus.feature.focussession.presentation.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.presentation.FocusSessionViewModel
import kotlinx.coroutines.launch
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.doOnPreDraw

class DetoxInterruptedFragment : Fragment(R.layout.fragment_detox_interrupted) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentDetoxInterruptedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetoxInterruptedBinding.bind(view)

        binding.btnStartAgain.setOnClickListener {
            viewModel.onRestartDetoxClick()
        }
        binding.btnToHome.setOnClickListener {
            viewModel.onDetoxInterruptedHomeClick()
        }

        observeUiState()
        observeEvents()

        if (viewModel.shouldPlayDetoxInterruptedAnimation()) {
            playDetoxInterruptedEnterAnimation()
        } else {
            showDetoxInterruptedWithoutAnimation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvDetoxTime1.text = formatSeconds(state.detoxUiState.detoxElapsedSeconds)
                    binding.tvDetoxTime2.text = getString(R.string.detox_interrupted_time_message, formatSeconds(state.detoxUiState.selectedDetoxSeconds))
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateToDetoxTimer -> {
                            findNavController().navigate(R.id.actionDetoxInterruptedFragmentToDetoxTimerFragment)
                        }
                        is FocusSessionEvent.NavigateHome -> {
                            findNavController().popBackStack(
                                R.id.homeFragment,
                                false
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun playDetoxInterruptedEnterAnimation() = with(binding) {
        val views = listOf(
            tvDetoxSession,
            viewCloseRed,
            tvDetoxEnded,
            cardDetoxResult,
            viewGrayLine1,
            tvAgain,
            viewGrayLine2,
            tvAgainText1,
            tvAgainText2,
            btnStartAgain,
            btnToHome
        )

        views.forEach { view ->
            view.alpha = 0f
            view.translationY = 32f
        }

        cardDetoxResult.scaleX = 0.96f
        cardDetoxResult.scaleY = 0.96f

        flIcon.alpha = 0f
        flIcon.scaleX = 0.75f
        flIcon.scaleY = 0.75f

        ivIcon.alpha = 0f
        ivIcon.scaleX = 0.7f
        ivIcon.scaleY = 0.7f

        root.doOnPreDraw {
            tvDetoxSession.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()

            viewCloseRed.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()

            tvDetoxEnded.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(140)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardDetoxResult.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(260)
                .setDuration(450)
                .setInterpolator(OvershootInterpolator(1.05f))
                .start()

            flIcon.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(420)
                .setDuration(360)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()

            ivIcon.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(520)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(1.6f))
                .start()

            val againViews = listOf(
                viewGrayLine1,
                tvAgain,
                viewGrayLine2
            )

            againViews.forEachIndexed { index, view ->
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(680L + index * 50L)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }

            tvAgainText1.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(820)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            tvAgainText2.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(920)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            btnStartAgain.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(1050)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(1.05f))
                .start()

            btnToHome.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(1150)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(1.05f))
                .start()
        }
    }

    private fun showDetoxInterruptedWithoutAnimation() = with(binding) {
        val views = listOf(
            tvDetoxSession,
            viewCloseRed,
            tvDetoxEnded,
            cardDetoxResult,
            viewGrayLine1,
            tvAgain,
            viewGrayLine2,
            tvAgainText1,
            tvAgainText2,
            btnStartAgain,
            btnToHome,
            flIcon,
            ivIcon
        )

        views.forEach { view ->
            view.animate().cancel()
            view.alpha = 1f
            view.translationY = 0f
            view.scaleX = 1f
            view.scaleY = 1f
        }
    }


}