package com.firstresources.learningactivity.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.firstresources.learningactivity.databinding.ActivitySplashBinding
import com.firstresources.learningactivity.ui.login.LoginActivity
import com.firstresources.learningactivity.ui.main.MainActivity
import com.firstresources.learningactivity.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initial state for smooth staggered animation
        binding.layoutBranding.alpha = 0f
        binding.layoutBranding.translationY = 50f
        binding.layoutBranding.scaleX = 0.9f
        binding.layoutBranding.scaleY = 0.9f

        binding.layoutBottom.alpha = 0f

        // Animate branding entrance
        binding.layoutBranding.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Animate bottom section entrance
        binding.layoutBottom.animate()
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(350)
            .start()

        // 2.2 second display delay, then navigate based on session
        Handler(Looper.getMainLooper()).postDelayed({
            val sessionManager = SessionManager(this)
            val nextIntent = if (sessionManager.isLoggedIn()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(nextIntent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2200)
    }
}
