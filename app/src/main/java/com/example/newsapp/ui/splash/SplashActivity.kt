package com.example.newsapp.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.newsapp.databinding.ActivitySplashBinding
import com.example.newsapp.ui.base.BaseActivity
import com.example.newsapp.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemNavigationBar()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            delay(1500)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}
