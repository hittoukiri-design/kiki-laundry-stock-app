package com.laundry.stockapp.ui.screens.outlet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laundry.stockapp.data.model.Outlet
import com.laundry.stockapp.data.model.Transaction
import com.laundry.stockapp.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Date

data class OutletListState(
    val outlets: List<Outlet> = emptyList(),
    val outletsByRegion: Map<String, List<Outlet>> = emptyMap(),
    val outletStats: Map<String, Pair<Int, Int>> = emptyMap(), // Maps outletId to Pair(totalQtyOut, transactionCount)
    val isLoading: Boolean = false,
    val error: String? = null
)

data class OutletDetailState(
    val outlet: Outlet? = null,
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalQtyOut: Int = 0,
    
    // Pemeliharaan
    val maintenanceItems: List<com.laundry.stockapp.data.model.MaintenanceItem> = emptyList(),
    val regulatorCheck: com.laundry.stockapp.data.model.SafetyRegulatorCheck? = null,
    val aparCheck: com.laundry.stockapp.data.model.SafetyAparCheck? = null
)

@HiltViewModel
class OutletViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val authRepository: com.laundry.stockapp.data.repository.AuthRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val settingsRepository: com.laundry.stockapp.data.repository.SettingsRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(OutletListState())
    val listState: StateFlow<OutletListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(OutletDetailState())
    val detailState: StateFlow<OutletDetailState> = _detailState.asStateFlow()

    val userRole = authRepository.loggedInUserRole

    init {
        loadOutlets()
    }

    fun loadOutlets() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)
            try {
                val outlets = repository.getOutlets()
                val allTransactions = try { repository.getTransactions() } catch (e: Exception) { emptyList() }
                val statsMap = allTransactions.groupBy { it.outletId.orEmpty() }.mapValues { (_, transList) ->
                    val totalQty = transList.sumOf { it.qtyOut ?: 0 }
                    val count = transList.size
                    Pair(totalQty, count)
                }
                // Normalize region to title case so SELATAN/Utara/barat all map correctly
                val regionMap = mapOf("SELATAN" to "Selatan", "UTARA" to "Utara", "BARAT" to "Barat")
                val normalizedOutlets = outlets.map { o ->
                    val oRegion = o.region.orEmpty()
                    val normalized = regionMap[oRegion.uppercase()] ?: oRegion.replaceFirstChar { it.uppercase() }
                    o.copy(region = normalized)
                }
                val grouped = normalizedOutlets.groupBy { it.region.orEmpty() }
                _listState.value = _listState.value.copy(
                    outlets = normalizedOutlets,
                    outletsByRegion = grouped,
                    outletStats = statsMap,
                    isLoading = false
                )
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun saveOutlet(name: String, region: String) {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)
            try {
                val newOutlet = Outlet(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    region = region
                )
                repository.saveOutlet(newOutlet)
                loadOutlets() // Refresh
                
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
                _listState.value = _listState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadOutletDetail(outletId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, error = null)
            try {
                var outlet = listState.value.outlets.find { it.id == outletId }
                if (outlet == null) {
                    val outlets = repository.getOutlets()
                    // Normalize region to title case for this fallback as well
                    val regionMap = mapOf("SELATAN" to "Selatan", "UTARA" to "Utara", "BARAT" to "Barat")
                    outlet = outlets.find { it.id == outletId }?.let { o ->
                        val oRegion = o.region.orEmpty()
                        val normalized = regionMap[oRegion.uppercase()] ?: oRegion.replaceFirstChar { it.uppercase() }
                        o.copy(region = normalized)
                    }
                }
                
                val transactions = repository.getTransactionsByOutlet(outletId)
                val totalQty = transactions.sumOf { it.qtyOut ?: 0 }

                // Load weekly maintenance items
                var mItems = repository.getMaintenanceItems(outletId)
                if (mItems.isEmpty()) {
                    // Seed the default 4 items requested by user
                    val defaultNames = listOf(
                        "Kipas Angin",
                        "Kebersihan Area Belakang Mesin",
                        "Rolling Door / Pintu Depan",
                        "Bagian Bawah Mesin"
                    )
                    for (name in defaultNames) {
                        repository.saveMaintenanceItem(
                            outletId,
                            com.laundry.stockapp.data.model.MaintenanceItem(name = name)
                        )
                    }
                    mItems = repository.getMaintenanceItems(outletId)
                }

                // Load safety checks
                val regulator = repository.getRegulatorCheck(outletId)
                val apar = repository.getAparCheck(outletId)

                _detailState.value = _detailState.value.copy(
                    outlet = outlet,
                    transactions = transactions,
                    filteredTransactions = transactions,
                    totalQtyOut = totalQty,
                    maintenanceItems = mItems,
                    regulatorCheck = regulator,
                    aparCheck = apar,
                    isLoading = false
                )
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun saveMaintenanceItem(outletId: String, name: String, itemId: String = "") {
        viewModelScope.launch {
            try {
                val item = com.laundry.stockapp.data.model.MaintenanceItem(
                    id = itemId,
                    name = name
                )
                repository.saveMaintenanceItem(outletId, item)
                kotlinx.coroutines.delay(200)
                loadOutletDetail(outletId) // Reload
                triggerSilentLocalBackup()
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(error = e.message)
            }
        }
    }

    fun deleteMaintenanceItem(outletId: String, itemId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMaintenanceItem(outletId, itemId)
                kotlinx.coroutines.delay(200)
                loadOutletDetail(outletId) // Reload
                triggerSilentLocalBackup()
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(error = e.message)
            }
        }
    }

    fun checkMaintenanceItem(outletId: String, itemId: String) {
        viewModelScope.launch {
            try {
                val current = _detailState.value.maintenanceItems.find { it.id == itemId }
                if (current != null) {
                    val isChecked = current.lastMaintenanceAt != null
                    val updated = current.copy(lastMaintenanceAt = if (isChecked) null else Date())
                    repository.saveMaintenanceItem(outletId, updated)
                    kotlinx.coroutines.delay(200)
                    loadOutletDetail(outletId) // Reload
                    triggerSilentLocalBackup()
                }
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(error = e.message)
            }
        }
    }

    fun saveRegulatorCheck(outletId: String, day: Int, month: Int, year: Int) {
        viewModelScope.launch {
            try {
                val check = com.laundry.stockapp.data.model.SafetyRegulatorCheck(
                    lastTestDay = day,
                    lastTestMonth = month,
                    lastTestYear = year
                )
                repository.saveRegulatorCheck(outletId, check)
                kotlinx.coroutines.delay(200)
                loadOutletDetail(outletId) // Reload
                triggerSilentLocalBackup()
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(error = e.message)
            }
        }
    }

    fun saveAparCheck(outletId: String, lastRefillDate: Date, intervalMonths: Int) {
        viewModelScope.launch {
            try {
                val check = com.laundry.stockapp.data.model.SafetyAparCheck(
                    lastRefillDate = lastRefillDate,
                    intervalMonths = intervalMonths
                )
                repository.saveAparCheck(outletId, check)
                kotlinx.coroutines.delay(200)
                loadOutletDetail(outletId) // Reload
                triggerSilentLocalBackup()
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(error = e.message)
            }
        }
    }

    private fun triggerSilentLocalBackup() {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.laundry.stockapp.util.BackupManager.triggerSilentLocalBackup(context, repository, settingsRepository)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
