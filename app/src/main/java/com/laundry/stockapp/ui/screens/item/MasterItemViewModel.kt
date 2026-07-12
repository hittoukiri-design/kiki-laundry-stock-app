package com.laundry.stockapp.ui.screens.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MasterItemState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showPasswordDialog: Boolean = false,
    val itemToDelete: Item? = null,
    val totalItem: Int = 0,
    val totalStartingStock: Int = 0,
    val totalOut: Int = 0,
    val statusSafeCount: Int = 0,
    val statusWarningCount: Int = 0,
    val statusDangerCount: Int = 0,
    val searchQuery: String = "",
)

@HiltViewModel
class MasterItemViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: com.laundry.stockapp.data.repository.AuthRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(MasterItemState())
    val state: StateFlow<MasterItemState> = _state.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    val userRole = authRepository.loggedInUserRole

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val items = repository.getItems()
                val totalItem = items.size
                val totalStartingStock = items.sumOf { it.startingStock ?: 0 }
                val totalOut = items.sumOf { it.totalOut ?: 0 }
                var statusSafeCount = 0
                var statusWarningCount = 0
                var statusDangerCount = 0
                items.forEach {
                    if (it.remainingStock <= 0) statusDangerCount++
                    else if (it.remainingStock < 5) statusWarningCount++
                    else statusSafeCount++
                }

                _state.value = _state.value.copy(
                    items = items,
                    isLoading = false,
                    totalItem = totalItem,
                    totalStartingStock = totalStartingStock,
                    totalOut = totalOut,
                    statusSafeCount = statusSafeCount,
                    statusWarningCount = statusWarningCount,
                    statusDangerCount = statusDangerCount
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun saveItem(item: Item) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                // Cek duplikat nama (case-insensitive), kecuali item yang sedang diedit
                val isDuplicate = _state.value.items.any { existing ->
                    existing.name.orEmpty().trim().equals(item.name.orEmpty().trim(), ignoreCase = true) &&
                    existing.id != item.id
                }
                if (isDuplicate) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Item \"${item.name.orEmpty().trim()}\" sudah terdaftar. Gunakan nama yang berbeda."
                    )
                    return@launch
                }
                repository.saveItem(item)
                kotlinx.coroutines.delay(200) // Allow local cache to update
                loadItems()
                
                // Silent local backup
                viewModelScope.launch {
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.laundry.stockapp.util.BackupManager.triggerSilentLocalBackup(context, repository, settingsRepository)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun requestDelete(item: Item) {
        _state.value = _state.value.copy(showPasswordDialog = true, itemToDelete = item)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(showPasswordDialog = false, itemToDelete = null)
    }

    fun confirmDelete(password: String) {
        val item = _state.value.itemToDelete ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, showPasswordDialog = false)
            val currentHash = settingsRepository.masterPasswordHash.firstOrNull()
            
            if (currentHash == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Master password is not set in settings!")
                return@launch
            }

            val isValid = settingsRepository.verifyMasterPassword(password, currentHash)
            if (isValid) {
                try {
                    repository.deleteItem(item.id)
                    loadItems()
                    
                    // Silent local backup
                    viewModelScope.launch {
                        try {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.laundry.stockapp.util.BackupManager.triggerSilentLocalBackup(context, repository, settingsRepository)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Incorrect Master Password")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
