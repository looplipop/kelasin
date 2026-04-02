package com.kelasin.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelasin.app.data.repository.AuthRepository
import com.kelasin.app.ui.main.MainActivity
import com.kelasin.app.ui.theme.KelasinTheme

class LoginActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(AuthRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KelasinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by viewModel.authState.collectAsStateWithLifecycle()
                    var currentScreen by remember { mutableStateOf("ROLES") }
                    var selectedRole by remember { mutableStateOf("MAHASIGMA") }

                    // Ketika login/register berhasil
                    LaunchedEffect(authState) {
                        if (authState is AuthState.Success) {
                            val userName = (authState as AuthState.Success).nama
                            if (currentScreen == "LOGIN") {
                                android.widget.Toast.makeText(this@LoginActivity, "Selamat Datang $userName", android.widget.Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            } else if (currentScreen == "REGISTER") {
                                android.widget.Toast.makeText(this@LoginActivity, "Registrasi berhasil. Silakan login.", android.widget.Toast.LENGTH_SHORT).show()
                                currentScreen = "LOGIN"
                                viewModel.resetState()
                            }
                        }
                    }

                    when (currentScreen) {
                        "ROLES" -> {
                            RoleSelectionScreen(
                                onRoleSelected = { role ->
                                    selectedRole = role
                                    currentScreen = "LOGIN"
                                },
                                onGoRegister = { role ->
                                    selectedRole = role
                                    currentScreen = "REGISTER"
                                }
                            )
                        }
                        "REGISTER" -> {
                            androidx.activity.compose.BackHandler {
                                viewModel.resetState()
                                currentScreen = "ROLES"
                            }
                            RegisterScreen(
                                authState = authState,
                                role = selectedRole,
                                onRegister = { nama, username, email, pass, confirm ->
                                    viewModel.register(nama, username, email, pass, confirm, selectedRole)
                                },
                                onBackToLogin = {
                                    viewModel.resetState()
                                    currentScreen = "LOGIN"
                                }
                            )
                        }
                        "LOGIN" -> {
                            LoginScreen(
                                authState = authState,
                                role = selectedRole,
                                onLogin = { identifier, pass -> viewModel.login(identifier, pass) },
                                onGoRegister = {
                                    viewModel.resetState()
                                    currentScreen = "REGISTER"
                                },
                                onBackToRoleSelection = {
                                    viewModel.resetState()
                                    currentScreen = "ROLES"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
