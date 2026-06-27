package com.example.boredomfocus.feature.home.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.databinding.FragmentHomeBinding
import com.example.boredomfocus.feature.sessionsettings.presentation.SessionSettingsBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.doOnPreDraw

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var homeAnimationHandled = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.btnStartDetox.setOnClickListener {
            SessionSettingsBottomSheet()
                .show(childFragmentManager, SessionSettingsBottomSheet.TAG)
        }
        setupSessionBottomSheetResult()

        observeUiState()

        if (!homeAnimationHandled) {
            homeAnimationHandled = true

            if (viewModel.shouldPlayHomeAnimation()) {
                playHomeEnterAnimation()
            } else {
                showHomeWithoutAnimation()
            }
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
                    if (state.isLoading) {
                        binding.homeContent.alpha = 0f
                        return@collect
                    }

                    binding.homeContent.alpha = 1f
                    render(state)
                }
            }
        }
    }

    private fun render(state: HomeUiState) {
        renderWeekCircles(state)

        binding.tvStreakCount.text = state.streakCount.toString()
        binding.tvSessionCount.text = state.sessionCount.toString()
        binding.tvStreak2.text = formatSeconds(state.focusRecord)

        if(state.todayStreak) {
            binding.tvStreakCounted.visibility = View.VISIBLE
            binding.viewStreakCounted.visibility = View.VISIBLE
            binding.btnStartDetox.text = "Ещё одна сессия"
        } else {
            binding.tvStreakCounted.visibility = View.GONE
            binding.viewStreakCounted.visibility = View.GONE
            binding.btnStartDetox.text = "Начать детокс"
        }
    }

    private fun renderWeekCircles(state: HomeUiState) {
        val days = listOf(
            binding.viewMonday,
            binding.viewTuesday,
            binding.viewWednesday,
            binding.viewThursday,
            binding.viewFriday,
            binding.viewSaturday,
            binding.viewSunday
        )

        days.forEachIndexed { index, view ->
            val entity = state.dailyStats.getOrNull(index)
            Log.d("HomeFragment", "renderWeekCircles: $entity")

            val background = when {
                entity == null -> R.drawable.circle_empty
                entity.streakCounted -> R.drawable.circle_filled
                else -> R.drawable.circle_missed
            }

            view.setBackgroundResource(background)
        }
    }

    private fun setupSessionBottomSheetResult() {
        childFragmentManager.setFragmentResultListener(
            SessionSettingsBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val detoxDuration =
                bundle.getSerializable(SessionSettingsBottomSheet.KEY_DETOX_DURATION) as? DetoxDuration
                    ?: DetoxDuration.FIVE_MINUTES

            val difficulty =
                bundle.getSerializable(SessionSettingsBottomSheet.KEY_DIFFICULTY) as? Difficulty
                    ?: Difficulty.BEGINNER

            val focusOnly = bundle.getBoolean(SessionSettingsBottomSheet.KEY_FOCUS_ONLY)

            val args = bundleOf(
                "detoxDuration" to detoxDuration,
                "difficulty" to difficulty,
                "focusOnly" to focusOnly
            )

            findNavController().navigate(
                R.id.actionHomeFragmentToFocusSessionGraph,
                args
            )
        }
    }

    private fun playHomeEnterAnimation() = with(binding) {
        val topViews = listOf(
            tvStreak1,
            tvStreak2,
            tvStreak3
        )

        val mainViews = listOf(
            cardWeek,
            viewStreakCounted,
            tvStreakCounted,
            btnStartDetox,
            cardRecord,
            cardSession,
            cardQuote
        )

        topViews.forEach { view ->
            view.alpha = 0f
            view.translationY = 28f
        }

        mainViews.forEach { view ->
            view.alpha = 0f
            view.translationY = 36f
        }

        tvStreak2.scaleX = 0.96f
        tvStreak2.scaleY = 0.96f

        cardRecord.scaleX = 0.96f
        cardRecord.scaleY = 0.96f

        cardSession.scaleX = 0.96f
        cardSession.scaleY = 0.96f

        cardQuote.scaleX = 0.98f
        cardQuote.scaleY = 0.98f

        root.doOnPreDraw {
            tvStreak1.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .start()

            tvStreak2.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(100)
                .setDuration(420)
                .setInterpolator(OvershootInterpolator(1.05f))
                .start()

            tvStreak3.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(220)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardWeek.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(340)
                .setDuration(320)
                .setInterpolator(DecelerateInterpolator())
                .start()

            viewStreakCounted.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(460)
                .setDuration(260)
                .setInterpolator(DecelerateInterpolator())
                .start()

            tvStreakCounted.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(480)
                .setDuration(260)
                .setInterpolator(DecelerateInterpolator())
                .start()

            btnStartDetox.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(580)
                .setDuration(360)
                .setInterpolator(OvershootInterpolator(1.04f))
                .start()

            cardRecord.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(700)
                .setDuration(330)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardSession.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(780)
                .setDuration(330)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardQuote.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(900)
                .setDuration(360)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showHomeWithoutAnimation() = with(binding) {
        val views = listOf(
            tvStreak1,
            tvStreak2,
            tvStreak3,
            cardWeek,
            viewStreakCounted,
            tvStreakCounted,
            btnStartDetox,
            cardRecord,
            cardSession,
            cardQuote
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