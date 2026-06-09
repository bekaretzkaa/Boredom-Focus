package com.example.boredomfocus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.boredomfocus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
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
//        startActivity(Intent(this, StopwatchActivity::class.java))
//        startActivity(Intent(this, DetoxTimerActivity::class.java))
        startActivity(Intent(this, OnboardingActivity::class.java))

        binding.btnStartDetox.setOnClickListener {
            startActivity(Intent(this, FocusResultActivity::class.java))
        }
    }
}