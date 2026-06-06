package com.example.boredomfocus

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
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

        val dotsIndicator1 = binding.dotsIndicatorOnboarding1
        dotsIndicator1.attachTo(binding.viewPagerOnboarding)
        val dotsIndicator2 = binding.dotsIndicatorOnboarding2
        dotsIndicator2.attachTo(binding.viewPagerOnboarding)

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                when(position) {
                    2 -> {
                        dotsIndicator1.visibility = View.GONE
                        dotsIndicator2.visibility = View.VISIBLE
                    }
                    else -> {
                        dotsIndicator1.visibility = View.VISIBLE
                        dotsIndicator2.visibility = View.GONE
                    }
                }
            }
        })
    }
}