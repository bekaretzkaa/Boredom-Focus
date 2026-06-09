package com.example.boredomfocus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

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

        val navHost = supportFragmentManager.findFragmentById(binding.mainFragmentContainer.id) as NavHostFragment
        navController = navHost.navController

        setupBottomNavigation()
        observeDestinations()
    }

    private fun observeDestinations() {
        navController.addOnDestinationChangedListener {
                _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> showBottomNav()
                R.id.statisticsFragment -> showBottomNav()
                R.id.settingsFragment -> showBottomNav()
                else -> hideBottomNav()
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
        
        navController.navigate(destinationId, null, navOptions)
    }

    private fun showBottomNav() {
        binding.bottomNavContainer.isVisible = true
    }

    private fun hideBottomNav() {
        binding.bottomNavContainer.isVisible = false
    }
}