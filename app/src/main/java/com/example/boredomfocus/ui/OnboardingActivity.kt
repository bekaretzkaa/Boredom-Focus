package com.example.boredomfocus.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.boredomfocus.OnboardingFragmentAdapter
import com.example.boredomfocus.databinding.ActivityOnboardingBinding
import com.example.boredomfocus.ui.onboardingFragments.OnboardingFragmentFirst
import com.example.boredomfocus.ui.onboardingFragments.OnboardingFragmentSecond
import com.example.boredomfocus.ui.onboardingFragments.OnboardingFragmentThird
import com.example.boredomfocus.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

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


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToPage.collect { pageIndex ->
                    binding.viewPagerOnboarding.setCurrentItem(pageIndex, true)
                }
            }
        }
    }
}