package com.example.boredomfocus.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.animation.LinearInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.ActivityDetoxTimerBinding
import kotlin.concurrent.timer

class DetoxTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetoxTimerBinding

    private var timerAnimator: ValueAnimator? = null

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
    }

    override fun onStart() {
        super.onStart()
        startTimer(22)
    }

    private fun startTimer(totalSeconds: Int) {

        timerAnimator?.cancel()

        val totalMillis = totalSeconds * 1000L

        timerAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = totalMillis
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                binding.progressViewDetoxTimer.progress = progress

                val millisLest = (totalMillis * progress).toLong()

                val totalSeconds = millisLest / 1000

                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60

                binding.tvDetoxTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            start()
        }
    }
}