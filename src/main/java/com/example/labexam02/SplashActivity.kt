package com.example.labexam02

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to your splash screen layout
        setContentView(R.layout.activity_splash)

        // Delay and launch the MainActivity
        Handler().postDelayed({
            // After delay, start the MainActivity
            val intent = Intent(
                this@SplashActivity,
                MainActivity::class.java
            )
            startActivity(intent)
            finish() // Close the splash activity
        }, 3000) // Splash screen delay time (3000 ms = 3 seconds)
    }
}