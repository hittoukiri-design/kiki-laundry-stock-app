package com.laundry.stockapp

import androidx.lifecycle.ViewModel
import com.laundry.stockapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

import com.laundry.stockapp.data.repository.SettingsRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    val isLoggedIn = authRepository.loggedInUserId.map { it != null }
    val userRole = authRepository.loggedInUserRole
    val userName = authRepository.loggedInUserName
    val profilePhone = settingsRepository.profilePhone
    val profileImageUri = settingsRepository.profileImageUri

    private var lastInteractionTime = System.currentTimeMillis()
    val isLocked = MutableStateFlow(false)
    val isAppLockEnabled = settingsRepository.appLockEnabled
    val appLockPin = settingsRepository.appLockPin

    init {
        // Idle timeout checker loop
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // check every 5 seconds
                val enabled = settingsRepository.appLockEnabled.firstOrNull() ?: false
                val pin = settingsRepository.appLockPin.firstOrNull()
                if (enabled && !pin.isNullOrEmpty() && !isLocked.value) {
                    val idleTime = System.currentTimeMillis() - lastInteractionTime
                    if (idleTime >= 30 * 60 * 1000) { // 30 minutes
                        isLocked.value = true
                    }
                }
            }
        }
    }

    fun updateInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    fun lockManually() {
        viewModelScope.launch {
            val enabled = settingsRepository.appLockEnabled.firstOrNull() ?: false
            val pin = settingsRepository.appLockPin.firstOrNull()
            if (enabled && !pin.isNullOrEmpty()) {
                isLocked.value = true
            }
        }
    }

    fun unlock(pin: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val correctPin = settingsRepository.appLockPin.firstOrNull()
            if (correctPin == pin) {
                isLocked.value = false
                updateInteraction()
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun saveProfile(phone: String, imageUri: String?) {
        viewModelScope.launch {
            settingsRepository.setProfilePhone(phone)
            if (imageUri != null) {
                if (imageUri.startsWith("content://")) {
                    try {
                        val uri = imageUri.toUri()
                        val inputStream = context.contentResolver.openInputStream(uri)
                        
                        // Delete old profile pictures to save space
                        context.filesDir.listFiles { _, name -> name.startsWith("profile_picture_") }?.forEach { it.delete() }
                        
                        val timestamp = System.currentTimeMillis()
                        val file = File(context.filesDir, "profile_picture_$timestamp.jpg")
                        val outputStream = FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                        settingsRepository.setProfileImageUri(android.net.Uri.fromFile(file).toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    settingsRepository.setProfileImageUri(imageUri)
                }
            }
        }
    }

    @Suppress("unused")
    suspend fun logout() {
        authRepository.logout()
    }
}
