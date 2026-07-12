package com.laundry.stockapp.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laundry.stockapp.data.model.User
import com.laundry.stockapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(email: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                authRepository.login(cleanEmail, cleanPass)
                _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Akses ditolak oleh Firebase! Database Anda terkunci. Silakan ubah Rules di Firebase Console."
                } else {
                    e.message
                }
                _state.value = _state.value.copy(isLoading = false, error = errorMsg)
            }
        }
    }

    fun register(name: String, email: String, phone: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val user = User(
                    name = name,
                    email = cleanEmail,
                    phone = phone,
                    passwordHash = cleanPass // Will be hashed in repository
                )
                authRepository.register(user)
                _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Akses ditolak oleh Firebase! Database Anda terkunci (Rules kadaluarsa/production mode). Silakan buka Firebase Console -> Firestore Database -> Rules, lalu ubah menjadi 'allow read, write: if true;'"
                } else {
                    e.message
                }
                _state.value = _state.value.copy(isLoading = false, error = errorMsg)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
