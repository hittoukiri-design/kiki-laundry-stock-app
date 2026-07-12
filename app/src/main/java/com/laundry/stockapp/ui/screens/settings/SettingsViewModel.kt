package com.laundry.stockapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.SettingsRepository
import com.laundry.stockapp.data.repository.AuthRepository
import com.laundry.stockapp.util.ExcelExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.documentfile.provider.DocumentFile
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri

data class SettingsState(
    val hasMasterPassword: Boolean = false,
    val isBackupEnabled: Boolean = false,
    val googleDriveEmail: String? = null,
    val googleDriveFolder: String? = null,
    val googleDriveFolderId: String? = null,
    val exportEmail: String = "",
    val lastBackupAt: String? = null,
    val isLoading: Boolean = false,
    val isGoogleDriveConnecting: Boolean = false,
    val isGoogleDriveConnected: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val exportSuccessMessage: String? = null,
    val syncProgress: Int? = null,
    val lastBackupSize: String? = null,
    val isAppLockEnabled: Boolean = false,
    val appLockPin: String? = null,
    val lastLocalBackupAt: String? = null,
    val lastLocalBackupSize: String? = null,
    val backupHistory: List<com.laundry.stockapp.data.model.BackupLog> = emptyList(),
    val recoveryIntent: android.content.Intent? = null,
    val waBotQrCode: String? = null,
    val waBotConnected: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val firestoreRepository: FirestoreRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.masterPasswordHash.collect { hash ->
                _state.value = _state.value.copy(hasMasterPassword = hash != null)
            }
        }
        viewModelScope.launch {
            settingsRepository.backupEnabled.collect { enabled ->
                _state.value = _state.value.copy(isBackupEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.googleDriveEmail.collect { email ->
                _state.value = _state.value.copy(googleDriveEmail = email)
                // Auto-fill export email if empty when drive is connected
                if (!email.isNullOrBlank() && _state.value.exportEmail.isBlank()) {
                    setExportEmail(email)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.googleDriveFolder.collect { folder ->
                _state.value = _state.value.copy(googleDriveFolder = folder)
            }
        }
        viewModelScope.launch {
            settingsRepository.googleDriveFolderId.collect { id ->
                _state.value = _state.value.copy(googleDriveFolderId = id)
            }
        }
        viewModelScope.launch {
            settingsRepository.exportEmail.collect { email ->
                if (!email.isNullOrBlank()) {
                    _state.value = _state.value.copy(exportEmail = email)
                } else {
                    // Fallback to google drive email if export email is blank
                    val driveEmail = _state.value.googleDriveEmail
                    if (!driveEmail.isNullOrBlank()) {
                        setExportEmail(driveEmail)
                    }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.lastBackupAt.collect { time ->
                _state.value = _state.value.copy(lastBackupAt = time)
            }
        }
        viewModelScope.launch {
            settingsRepository.lastBackupSize.collect { size ->
                _state.value = _state.value.copy(lastBackupSize = size)
            }
        }
        viewModelScope.launch {
            settingsRepository.lastLocalBackupAt.collect { time ->
                _state.value = _state.value.copy(lastLocalBackupAt = time)
            }
        }
        viewModelScope.launch {
            settingsRepository.lastLocalBackupSize.collect { size ->
                _state.value = _state.value.copy(lastLocalBackupSize = size)
            }
        }
        viewModelScope.launch {
            settingsRepository.appLockEnabled.collect { enabled ->
                _state.value = _state.value.copy(isAppLockEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.appLockPin.collect { pin ->
                _state.value = _state.value.copy(appLockPin = pin)
            }
        }
        loadBackupHistory()
        startPollingWaBotStatus()
    }

    private var waPollingJob: kotlinx.coroutines.Job? = null

    private fun startPollingWaBotStatus() {
        waPollingJob?.cancel()
        waPollingJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while(true) {
                try {
                    val url = java.net.URL("https://wa-jcl-bot-138299760138.asia-southeast2.run.app/api/wa/status")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val jsonObject = org.json.JSONObject(response)
                        val isConnected = jsonObject.optBoolean("isConnected", false)
                        val qr = if (jsonObject.has("qr") && !jsonObject.isNull("qr")) jsonObject.optString("qr") else null
                        
                        _state.value = _state.value.copy(
                            waBotConnected = isConnected,
                            waBotQrCode = qr
                        )
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun loadBackupHistory() {
        viewModelScope.launch {
            try {
                val history = firestoreRepository.getBackupHistory()
                _state.value = _state.value.copy(backupHistory = history)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setMasterPassword(password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                settingsRepository.setMasterPassword(password)
                authRepository.updateMasterPasswordInCloud(password)
                _state.value = _state.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBackupEnabled(enabled)
            if (enabled) {
                com.laundry.stockapp.util.BackupManager.schedulePeriodicBackup(context)
            } else {
                com.laundry.stockapp.util.BackupManager.cancelPeriodicBackup(context)
            }
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAppLockEnabled(enabled)
        }
    }

    fun setAppLockPin(pin: String) {
        viewModelScope.launch {
            settingsRepository.setAppLockPin(pin)
        }
    }

    fun setGoogleDriveEmail(email: String?) {
        viewModelScope.launch {
            settingsRepository.setGoogleDriveEmail(email)
        }
    }

    fun setGoogleDriveFolder(folder: String?, folderId: String?) {
        viewModelScope.launch {
            settingsRepository.setGoogleDriveFolder(folder, folderId)
        }
    }

    fun setExportEmail(email: String) {
        viewModelScope.launch {
            settingsRepository.setExportEmail(email)
        }
    }

    fun exportDataToExcel() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val items = firestoreRepository.getItems()
                val outlets = firestoreRepository.getOutlets()
                val transactions = firestoreRepository.getTransactions()
                
                val file = ExcelExporter.exportToExcel(context, items, outlets, transactions)
                if (file != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        exportSuccessMessage = "Data berhasil diexport ke:\n${file.absolutePath}"
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal membuat file Excel.")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, syncProgress = 0)
            try {
                // Generate Excel first
                val items = firestoreRepository.getItems()
                val outlets = firestoreRepository.getOutlets()
                val transactions = firestoreRepository.getTransactions()
                
                // Simulate progress for data fetching
                _state.value = _state.value.copy(syncProgress = 30)
                kotlinx.coroutines.delay(500)
                
                val file = ExcelExporter.exportToExcel(context, items, outlets, transactions)
                _state.value = _state.value.copy(syncProgress = 60)
                kotlinx.coroutines.delay(500)
                
                if (file != null) {
                    var successMessage = "Data tersinkronisasi dan file Excel berhasil dibuat."
                    var isSuccess = true
                    var errMessage: String? = null
                    
                    // Google Drive upload if connected
                    val driveEmail = _state.value.googleDriveEmail
                    if (!driveEmail.isNullOrEmpty()) {
                        try {
                            _state.value = _state.value.copy(syncProgress = 70)
                            val token = com.laundry.stockapp.util.GoogleDriveHelper.getAccessToken(context, driveEmail)
                            
                            _state.value = _state.value.copy(syncProgress = 80)
                            val folderId = com.laundry.stockapp.util.GoogleDriveHelper.findOrCreateAppFolder(token)
                            
                            _state.value = _state.value.copy(syncProgress = 90)
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val uploadName = "Laporan_Stok_LONDRI_$timestamp.xlsx"
                            
                            val uploaded = com.laundry.stockapp.util.GoogleDriveHelper.uploadBackupFile(token, folderId, file, uploadName)
                            if (uploaded) {
                                successMessage = "Data berhasil disinkronkan dan di-backup ke Google Drive folder: Kiki's Laundry Stock App"
                            } else {
                                successMessage = "Data sinkron, tetapi gagal mengunggah ke Google Drive."
                                isSuccess = false
                                errMessage = "Upload returned false"
                            }
                        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                            e.printStackTrace()
                            successMessage = "Data sinkron, tetapi membutuhkan izin Google Drive."
                            isSuccess = false
                            errMessage = e.message
                            _state.value = _state.value.copy(recoveryIntent = e.intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            successMessage = "Data sinkron, tetapi gagal backup ke Google Drive: ${e.message}"
                            isSuccess = false
                            errMessage = e.message
                        }
                    }

                    // If a folder has been selected (Google Drive / Local SAF), copy the file there
                    val folderUriString = _state.value.googleDriveFolderId
                    if (!folderUriString.isNullOrEmpty() && folderUriString != "drive_folder_auto_id") {
                        try {
                            val treeUri = Uri.parse(folderUriString)
                            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                            if (documentFile != null && documentFile.canWrite()) {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val newFile = documentFile.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Laporan_Stok_LONDRI_$timestamp.xlsx")
                                
                                if (newFile != null) {
                                    val outputStream = context.contentResolver.openOutputStream(newFile.uri)
                                    val inputStream = FileInputStream(file)
                                    inputStream.copyTo(outputStream!!)
                                    inputStream.close()
                                    outputStream.close()
                                    if (driveEmail.isNullOrEmpty()) {
                                        successMessage = "Data berhasil disinkronkan dan di-backup ke:\n${_state.value.googleDriveFolder}"
                                    } else {
                                        successMessage += "\n& di-backup juga ke folder lokal: ${_state.value.googleDriveFolder}"
                                    }
                                } else {
                                    if (driveEmail.isNullOrEmpty()) {
                                        successMessage = "Data sinkron, tetapi gagal membuat file di folder pilihan (mungkin izin akses dicabut)."
                                    }
                                }
                            } else {
                                if (driveEmail.isNullOrEmpty()) {
                                    successMessage = "Data sinkron, tetapi tidak ada izin akses ke folder pilihan. Silakan pilih folder ulang."
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (driveEmail.isNullOrEmpty()) {
                                successMessage = "Data sinkron, tetapi gagal menyimpan ke folder pilihan: ${e.message}"
                            }
                        }
                    }

                    // Update last backup timestamp
                    val currentTime = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())
                    settingsRepository.setLastBackupAt(currentTime)
                    
                    val fileSizeKb = file.length() / 1024
                    val sizeString = if (fileSizeKb > 1024) String.format(Locale.getDefault(), "%.2f MB", fileSizeKb / 1024f) else "$fileSizeKb KB"

                    // Save backup log to Firestore
                    try {
                        val operatorName = authRepository.loggedInUserName.firstOrNull() ?: "Admin"
                        val log = com.laundry.stockapp.data.model.BackupLog(
                            timeFormatted = currentTime,
                            type = "Manual",
                            size = sizeString,
                            status = if (isSuccess) "Berhasil" else "Gagal",
                            errorMessage = errMessage,
                            operator = operatorName
                        )
                        firestoreRepository.saveBackupLog(log)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    _state.value = _state.value.copy(syncProgress = 100)
                    kotlinx.coroutines.delay(500) // let UI show 100%
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        syncProgress = null,
                        lastBackupSize = sizeString,
                        exportSuccessMessage = successMessage
                    )
                    loadBackupHistory()
                } else {
                    _state.value = _state.value.copy(isLoading = false, syncProgress = null, error = "Gagal membuat file Excel untuk backup.")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun connectToGoogleDrive(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGoogleDriveConnecting = true)
            setGoogleDriveEmail(email)
            setGoogleDriveFolder("Kiki's Laundry Stock App", "drive_folder_auto_id")
            _state.value = _state.value.copy(
                isGoogleDriveConnecting = false,
                isGoogleDriveConnected = true,
                isSuccess = true
            )
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            setGoogleDriveEmail(null)
            setGoogleDriveFolder(null, null)
            _state.value = _state.value.copy(
                isGoogleDriveConnected = false
            )
        }
    }

    fun clearExportSuccess() {
        _state.value = _state.value.copy(exportSuccessMessage = null)
    }

    fun clearRecoveryIntent() {
        _state.value = _state.value.copy(recoveryIntent = null)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(isSuccess = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetAllData(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val currentHash = settingsRepository.masterPasswordHash.firstOrNull()
            
            if (currentHash == null) {
                _state.value = _state.value.copy(isLoading = false)
                onError("Master password belum diatur di menu Settings!")
                return@launch
            }

            val isValid = settingsRepository.verifyMasterPassword(password, currentHash)
            if (isValid) {
                try {
                    firestoreRepository.clearAllCollections()
                    // Reset backup settings locally
                    settingsRepository.setLastBackupAt("")
                    settingsRepository.setBackupEnabled(false)
                    settingsRepository.setGoogleDriveFolder(null, null)
                    settingsRepository.setGoogleDriveEmail(null)
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        googleDriveEmail = null,
                        googleDriveFolder = null,
                        googleDriveFolderId = null,
                        isBackupEnabled = false,
                        lastBackupAt = null
                    )
                    onSuccess()
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                    onError(e.message ?: "Gagal melakukan reset data")
                }
            } else {
                _state.value = _state.value.copy(isLoading = false)
                onError("Master Password salah")
            }
        }
    }

    fun resetAppPinWithMasterPassword(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val currentHash = settingsRepository.masterPasswordHash.firstOrNull()
            if (currentHash == null) {
                onError("Master password belum diatur di menu Keamanan!")
                return@launch
            }
            val isValid = settingsRepository.verifyMasterPassword(password, currentHash)
            if (isValid) {
                try {
                    settingsRepository.setAppLockPin("")
                    _state.value = _state.value.copy(appLockPin = null)
                    onSuccess()
                } catch (e: Exception) {
                    onError(e.message ?: "Gagal mereset PIN")
                }
            } else {
                onError("Master Password salah")
            }
        }
    }
}
