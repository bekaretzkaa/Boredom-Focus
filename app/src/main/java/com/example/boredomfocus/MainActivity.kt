package com.example.boredomfocus

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
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
}