package com.laundry.stockapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.laundry.stockapp.data.model.Transaction
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.AuthRepository
import com.laundry.stockapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val totalActiveItems: Int = 0,
    val totalStartingStock: Int = 0,
    val totalOut: Int = 0,
    val totalRemainingStock: Int = 0,
    val statusSafeCount: Int = 0,
    val statusWarningCount: Int = 0,
    val statusDangerCount: Int = 0,
    val recentTransactions: List<Transaction> = emptyList(),
    val userName: String = "Pengguna",
    val lastBackupAt: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    private var transactionListener: ListenerRegistration? = null

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    init {
        loadDashboardData()
        transactionListener = repository.listenToTransactions {
            loadDashboardData()
        }
        viewModelScope.launch {
            authRepository.loggedInUserName.collect { name ->
                _state.value = _state.value.copy(userName = name ?: "Pengguna")
            }
        }
        viewModelScope.launch {
            settingsRepository.lastBackupAt.collect { backupAt ->
                _state.value = _state.value.copy(lastBackupAt = backupAt)
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val items = repository.getItems()
                val activeItems = items.size
                val startingStock = items.sumOf { it.startingStock ?: 0 }
                val outStock = items.sumOf { it.totalOut ?: 0 }
                val remainingStock = items.sumOf { it.remainingStock }
                val safeCount = items.count { it.remainingStock >= 5 }
                val warningCount = items.count { it.remainingStock in 1..4 }
                val dangerCount = items.count { it.remainingStock <= 0 }
                val transactions = repository.getRecentTransactions(limit = 5)
                _state.value = _state.value.copy(
                    totalActiveItems = activeItems,
                    totalStartingStock = startingStock,
                    totalOut = outStock,
                    totalRemainingStock = remainingStock,
                    statusSafeCount = safeCount,
                    statusWarningCount = warningCount,
                    statusDangerCount = dangerCount,
                    recentTransactions = transactions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    override fun onCleared() {
        transactionListener?.remove()
        transactionListener = null
        super.onCleared()
    }
}
