package com.dicoding.asclepius.presentation.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Splash : AppCompatActivity() {
    private var splashJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashJob?.cancel()
        splashJob = lifecycleScope.launch {
            delay(SPLASH_DELAY)
            val intent = Intent(this@Splash, MainActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        private const val SPLASH_DELAY = 1500L
    }
}