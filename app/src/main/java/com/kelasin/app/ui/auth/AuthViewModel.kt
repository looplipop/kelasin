package com.kelasin.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kelasin.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String, val nama: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authRepo: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email/username dan password tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepo.login(identifier.trim(), password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it.id.toString(), it.nama) },
                onFailure = { AuthState.Error(it.message ?: "Login gagal") }
            )
        }
    }

    fun register(nama: String, username: String, email: String, password: String, confirmPassword: String, role: String) {
        if (nama.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Semua kolom wajib diisi")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Konfirmasi password tidak cocok")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password minimal 6 karakter")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepo.register(nama.trim(), username.trim().lowercase(), email.trim(), password, role)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it.id.toString(), it.nama) },
                onFailure = { AuthState.Error(it.message ?: "Registrasi gagal") }
            )
        }
    }

    fun resetState() { _authState.value = AuthState.Idle }

    class Factory(private val authRepo: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepo) as T
        }
    }
}
