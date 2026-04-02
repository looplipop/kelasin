package com.kelasin.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kelasin.app.data.repository.AuthRepository
import com.kelasin.app.ui.auth.LoginActivity
import com.kelasin.app.ui.main.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authRepo = AuthRepository(this)

        lifecycleScope.launch {
            authRepo.restoreSessionToken()
            val userId = authRepo.currentUserId.first()
            val dest = if (!userId.isNullOrBlank()) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }
            startActivity(dest)
            finish()
        }
    }
}
