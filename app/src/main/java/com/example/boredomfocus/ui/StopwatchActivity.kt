package com.example.boredomfocus.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.ActivityStopwatchBinding
import com.example.boredomfocus.viewmodel.StopwatchViewModel
import kotlinx.coroutines.launch

class StopwatchActivity : AppCompatActivity() {

    private val viewModel: StopwatchViewModel by viewModels()

    private lateinit var binding: ActivityStopwatchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityStopwatchBinding.inflate(layoutInflater)
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
            viewModel.start()
        }

        binding.btnStopFocus.setOnClickListener {
            viewModel.stop()
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.time.collect {
                        binding.tvStopwatch.text = it
                    }
                }

                launch {
                    viewModel.time.collect {
                        binding.tvTodayTime.text = "$it..."
                    }
                }
            }
        }
    }
}