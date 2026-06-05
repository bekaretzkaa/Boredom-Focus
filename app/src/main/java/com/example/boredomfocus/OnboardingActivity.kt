package com.example.boredomfocus

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.boredomfocus.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragments = listOf(
            OnboardingFragmentFirst(),
            OnboardingFragmentSecond(),
            OnboardingFragmentThird()
        )

        val adapter = OnboardingFragmentAdapter(
            fragments,
            supportFragmentManager,
            lifecycle
        )

        binding.viewPagerOnboarding.adapter = adapter
    }
}