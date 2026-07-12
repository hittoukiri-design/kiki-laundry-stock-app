package com.laundry.stockapp.ui.screens.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.SettingsRepository
import com.laundry.stockapp.util.ExcelExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportExcelState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val totalItems: Int = 0,
    val totalTransactions: Int = 0,
    val totalOutlets: Int = 0,
    val error: String? = null,
    val defaultEmail: String = "",
    val backupFolderUri: String? = null,
    val backupFolderName: String? = null
)

@HiltViewModel
class ExportExcelViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ExportExcelState())
    val state: StateFlow<ExportExcelState> = _state.asStateFlow()

    init {
        loadStats()
        viewModelScope.launch {
            settingsRepository.exportEmail.collect { email ->
                _state.update { it.copy(defaultEmail = email ?: "") }
            }
        }
        viewModelScope.launch {
            settingsRepository.googleDriveFolderId.collect { id ->
                _state.update { it.copy(backupFolderUri = id) }
            }
        }
        viewModelScope.launch {
            settingsRepository.googleDriveFolder.collect { folder ->
                _state.update { it.copy(backupFolderName = folder) }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val items = repository.getItems()
                val transactions = repository.getTransactions()
                val outlets = repository.getOutlets()
                _state.update {
                    it.copy(
                        totalItems = items.size,
                        totalTransactions = transactions.size,
                        totalOutlets = outlets.size
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    suspend fun getExportedFile(context: Context): java.io.File? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val items = repository.getItems()
            val outlets = repository.getOutlets()
            val transactions = repository.getTransactions()
            ExcelExporter.exportToExcel(context, items, outlets, transactions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportExcel(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val file = getExportedFile(context)
                if (file != null) {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Gagal membuat file Excel") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearSuccess() {
        _state.update { it.copy(isSuccess = false, error = null) }
    }
}
