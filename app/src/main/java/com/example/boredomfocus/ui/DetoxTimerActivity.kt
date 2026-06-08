package com.example.boredomfocus.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.animation.LinearInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.ActivityDetoxTimerBinding
import com.example.boredomfocus.viewmodel.DetoxTimerViewModel
import kotlinx.coroutines.launch
import kotlin.concurrent.timer

class DetoxTimerActivity : AppCompatActivity() {

    private val viewModel: DetoxTimerViewModel by viewModels()

    private lateinit var binding: ActivityDetoxTimerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDetoxTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val customPadding = (24 * resources.displayMetrics.density).toInt()
            v.setPadding(
                systemBars.left + customPadding,
                systemBars.top + customPadding,
                systemBars.right + customPadding,
                systemBars.bottom
            )
            insets
        }

        observeUi()

        if(!viewModel.isRunning) {
            viewModel.startTimer(totalSeconds = 22)
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.progress.collect {
                        binding.progressViewDetoxTimer.progress = it
                    }
                }

                launch {
                    viewModel.time.collect {
                        binding.tvDetoxTimer.text = it
                    }
                }
            }
        }
    }
}