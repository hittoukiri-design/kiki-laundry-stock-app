package com.laundry.stockapp.ui.screens.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.data.model.Outlet
import com.laundry.stockapp.data.model.Transaction
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class InputTransactionState(
    val items: List<Item> = emptyList(),
    val outlets: List<Outlet> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InputTransactionViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    private val settingsRepository: com.laundry.stockapp.data.repository.SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InputTransactionState())
    val state: StateFlow<InputTransactionState> = _state.asStateFlow()
    private var loadFullTransactions = false

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    init {
        loadData()
    }

    fun loadData(fullTransactions: Boolean = loadFullTransactions) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val items = repository.getItems()
                val outlets = repository.getOutlets()
                val transactions = if (fullTransactions) {
                    repository.getTransactions()
                } else {
                    repository.getRecentTransactions(limit = 5)
                }
                _state.value = _state.value.copy(
                    items = items,
                    outlets = outlets,
                    recentTransactions = transactions.sortedByDescending { it.date },
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadHistoryData() {
        loadFullTransactions = true
        loadData(fullTransactions = true)
    }

    fun saveTransaction(
        date: Date,
        outlet: Outlet,
        item: Item,
        qty: Int,
        notes: String
    ) {
        if (qty <= 0) {
            _state.value = _state.value.copy(error = "Qty harus lebih dari 0")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val transaction = Transaction(
                    date = date,
                    outletId = outlet.id,
                    outletName = outlet.name,
                    region = outlet.region,
                    itemId = item.id,
                    itemName = item.name,
                    qtyOut = qty,
                    notes = notes
                )
                repository.saveTransaction(transaction)
                _state.value = _state.value.copy(isLoading = false, isSuccess = true)
                loadData()
                
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

    fun clearSuccess() {
        _state.value = _state.value.copy(isSuccess = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.deleteTransaction(transaction)
                _state.value = _state.value.copy(isLoading = false)
                loadData()
                
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
}
