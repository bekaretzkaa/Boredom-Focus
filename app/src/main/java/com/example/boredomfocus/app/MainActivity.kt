package com.example.boredomfocus.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.ActivityMainBinding
import com.example.boredomfocus.feature.onboarding.presentation.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()

    private var onboardingStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        observeOnboarding()

        val navHost = supportFragmentManager.findFragmentById(binding.mainFragmentContainer.id) as NavHostFragment
        navController = navHost.navController

        setupBottomNavigation()
        observeDestinations()
    }

    private fun observeOnboarding() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOnboardingCompleted.collect { isCompleted ->

                    if(isCompleted == null) return@collect

                    if(!isCompleted && !onboardingStarted) {

                        onboardingStarted = true

                        startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                    }

                }
            }
        }
    }

    private fun observeDestinations() {
        navController.addOnDestinationChangedListener {
                _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    showBottomNav()
                    selectHome()
                }
                R.id.statisticsFragment -> {
                    showBottomNav()
                    selectStatistics()
                }
                R.id.settingsFragment -> {
                    showBottomNav()
                    selectSettings()
                }
                else -> {
                    hideBottomNav()
                    clearSelection()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.llNavHome.setOnClickListener {
            navigateToTopLevel(R.id.homeFragment)
        }

        binding.llNavStatistics.setOnClickListener {
            navigateToTopLevel(R.id.statisticsFragment)
        }

        binding.llNavSettings.setOnClickListener {
            navigateToTopLevel(R.id.settingsFragment)
        }
    }

    private fun navigateToTopLevel(destinationId: Int) {
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
            .build()

        navController.navigate(resId = destinationId, args = null, navOptions = navOptions)
    }

    private fun showBottomNav() {
        binding.bottomNavContainer.isVisible = true
    }

    private fun hideBottomNav() {
        binding.bottomNavContainer.isVisible = false
    }

    private fun selectHome() {
        binding.llNavHome.isSelected = true
        binding.llNavStatistics.isSelected = false
        binding.llNavSettings.isSelected = false
    }

    private fun selectStatistics() {
        binding.llNavHome.isSelected = false
        binding.llNavStatistics.isSelected = true
        binding.llNavSettings.isSelected = false
    }

    private fun selectSettings() {
        binding.llNavHome.isSelected = false
        binding.llNavStatistics.isSelected = false
        binding.llNavSettings.isSelected = true
    }

    private fun clearSelection() {
        binding.llNavHome.isSelected = false
        binding.llNavStatistics.isSelected = false
        binding.llNavSettings.isSelected = false
    }
}