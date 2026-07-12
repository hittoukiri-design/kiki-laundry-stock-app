package com.laundry.stockapp.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import com.google.firebase.storage.FirebaseStorage
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.SettingsRepository
import com.laundry.stockapp.sync.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BackupManager {

    private const val MAX_LOCAL_BACKUP_FILES_PER_TYPE = 30

    /**
     * Silent local backup of database snapshots (JSON) and Excel sheet.
     * Writes to local internal directory and also user-selected SAF folder tree (if configured).
     */
    suspend fun triggerSilentLocalBackup(
        context: Context,
        firestoreRepository: FirestoreRepository,
        settingsRepository: SettingsRepository
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Fetch data
            val items = firestoreRepository.getItems()
            val outlets = firestoreRepository.getOutlets()
            val transactions = firestoreRepository.getTransactions()

            // 1. Build JSON Object
            val backupObj = JSONObject().apply {
                put("tanggal_backup", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                
                // items
                val itemsArray = JSONArray()
                items.forEach { item ->
                    itemsArray.put(JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("startingStock", item.startingStock)
                        put("totalOut", item.totalOut)
                        put("remainingStock", item.remainingStock)
                        put("createdAt", (item.createdAt?.time ?: Date().time))
                        put("updatedAt", (item.updatedAt?.time ?: Date().time))
                    })
                }
                put("items", itemsArray)

                // outlets
                val outletsArray = JSONArray()
                outlets.forEach { outlet ->
                    val mItems = firestoreRepository.getMaintenanceItems(outlet.id)
                    val regulator = firestoreRepository.getRegulatorCheck(outlet.id)
                    val apar = firestoreRepository.getAparCheck(outlet.id)

                    outletsArray.put(JSONObject().apply {
                        put("id", outlet.id)
                        put("name", outlet.name)
                        put("region", outlet.region)
                        put("createdAt", (outlet.createdAt?.time ?: Date().time))
                        put("updatedAt", (outlet.updatedAt?.time ?: Date().time))

                        // weekly checklist items
                        val mItemsArray = JSONArray()
                        mItems.forEach { item ->
                            mItemsArray.put(JSONObject().apply {
                                put("id", item.id)
                                put("name", item.name)
                                put("lastMaintenanceAt", item.lastMaintenanceAt?.time ?: -1)
                                put("createdAt", (item.createdAt?.time ?: Date().time))
                                put("updatedAt", (item.updatedAt?.time ?: Date().time))
                            })
                        }
                        put("maintenanceItems", mItemsArray)

                        // regulator check
                        if (regulator != null) {
                            put("regulatorCheck", JSONObject().apply {
                                put("lastTestDay", regulator.lastTestDay ?: -1)
                                put("lastTestMonth", regulator.lastTestMonth ?: -1)
                                put("lastTestYear", regulator.lastTestYear ?: -1)
                                put("updatedAt", (regulator.updatedAt?.time ?: Date().time))
                            })
                        }

                        // apar check
                        if (apar != null) {
                            put("aparCheck", JSONObject().apply {
                                put("lastRefillDate", (apar.lastRefillDate?.time ?: -1))
                                put("intervalMonths", apar.intervalMonths ?: 36)
                                put("updatedAt", (apar.updatedAt?.time ?: Date().time))
                            })
                        }
                    })
                }
                put("outlets", outletsArray)

                // transactions
                val transactionsArray = JSONArray()
                transactions.forEach { trans ->
                    transactionsArray.put(JSONObject().apply {
                        put("id", trans.id)
                        put("date", (trans.date?.time ?: Date().time))
                        put("outletId", trans.outletId)
                        put("outletName", trans.outletName)
                        put("region", trans.region)
                        put("itemId", trans.itemId)
                        put("itemName", trans.itemName)
                        put("qtyOut", trans.qtyOut)
                        put("notes", trans.notes ?: "")
                        put("createdAt", (trans.createdAt?.time ?: Date().time))
                        put("updatedAt", (trans.updatedAt?.time ?: Date().time))
                    })
                }
                put("transactions", transactionsArray)
            }

            // Create local backups folder
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            // Save JSON file
            val jsonFile = File(backupDir, "local_backup.json")
            jsonFile.writeText(backupObj.toString(2))

            // Save Excel file
            val excelFile = ExcelExporter.exportToExcel(context, items, outlets, transactions, firestoreRepository)
            val destExcelFile = File(backupDir, "local_backup.xlsx")
            if (excelFile != null && excelFile.exists()) {
                excelFile.copyTo(destExcelFile, overwrite = true)
            }

            // Calculate Size
            val fileSizeKb = destExcelFile.length() / 1024
            val sizeString = if (fileSizeKb > 1024) String.format(Locale.getDefault(), "%.2f MB", fileSizeKb / 1024f) else "$fileSizeKb KB"

            // Save metadata
            val currentTime = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())
            settingsRepository.setLastLocalBackupAt(currentTime)
            settingsRepository.setLastLocalBackupSize(sizeString)

            // 2. SAF local folder tree backup copy (if configured)
            val folderUriString = settingsRepository.googleDriveFolderId.firstOrNull()
            if (!folderUriString.isNullOrEmpty() && folderUriString != "drive_folder_auto_id") {
                try {
                    val treeUri = Uri.parse(folderUriString)
                    val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                    if (documentFile != null && documentFile.canWrite()) {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        
                        // Excel Copy
                        val newExcelFile = documentFile.createFile(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "Laporan_Stok_LONDRI_$timestamp.xlsx"
                        )
                        if (newExcelFile != null && excelFile != null && excelFile.exists()) {
                            context.contentResolver.openOutputStream(newExcelFile.uri)?.use { out ->
                                FileInputStream(excelFile).use { input ->
                                    input.copyTo(out)
                                }
                            }
                        }

                        // JSON Copy
                        val newJsonFile = documentFile.createFile("application/json", "LONDRI_Backup_$timestamp.json")
                        if (newJsonFile != null && jsonFile.exists()) {
                            context.contentResolver.openOutputStream(newJsonFile.uri)?.use { out ->
                                FileInputStream(jsonFile).use { input ->
                                    input.copyTo(out)
                                }
                            }
                        }

                        cleanupOldSafBackups(documentFile)
                    }
                } catch (safErr: Exception) {
                    safErr.printStackTrace()
                }
            }

            return@withContext jsonFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun cleanupOldSafBackups(documentFile: DocumentFile) {
        cleanupOldSafBackupsByPrefix(documentFile, "Laporan_Stok_LONDRI_", ".xlsx")
        cleanupOldSafBackupsByPrefix(documentFile, "LONDRI_Backup_", ".json")
    }

    private fun cleanupOldSafBackupsByPrefix(
        documentFile: DocumentFile,
        prefix: String,
        suffix: String
    ) {
        val backups = documentFile.listFiles()
            .filter { file ->
                val name = file.name.orEmpty()
                name.startsWith(prefix) && name.endsWith(suffix)
            }
            .sortedByDescending { it.lastModified() }

        backups
            .drop(MAX_LOCAL_BACKUP_FILES_PER_TYPE)
            .forEach { oldFile ->
                try {
                    oldFile.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    /**
     * Standby Cloud backup to Firebase Storage and Google Drive.
     */
    suspend fun triggerCloudBackup(
        context: Context,
        firestoreRepository: FirestoreRepository,
        settingsRepository: SettingsRepository
    ) = withContext(Dispatchers.IO) {
        // Run local backup first to compile fresh files
        val jsonFile = triggerSilentLocalBackup(context, firestoreRepository, settingsRepository) ?: return@withContext

        val backupDir = File(context.filesDir, "backups")
        val excelFile = File(backupDir, "local_backup.xlsx")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        // 1. Upload JSON to Firebase Storage
        try {
            val storageInstance = FirebaseStorage.getInstance()
            val storageRef = storageInstance.reference.child("backups/LONDRI_Backup_$timestamp.json")
            if (jsonFile.exists()) {
                storageRef.putFile(Uri.fromFile(jsonFile)).await()
            }
        } catch (fErr: Exception) {
            fErr.printStackTrace()
        }

        // 2. Upload Excel to Google Drive (if user connected account)
        val driveEmail = settingsRepository.googleDriveEmail.firstOrNull()
        var isSuccess = true
        var errorMessage: String? = null

        if (!driveEmail.isNullOrEmpty()) {
            try {
                val token = GoogleDriveHelper.getAccessToken(context, driveEmail)
                val folderId = GoogleDriveHelper.findOrCreateAppFolder(token)
                val uploadName = "Laporan_Stok_LONDRI_$timestamp.xlsx"
                if (excelFile.exists()) {
                    val uploaded = GoogleDriveHelper.uploadBackupFile(token, folderId, excelFile, uploadName)
                    if (!uploaded) {
                        isSuccess = false
                        errorMessage = "Upload returned false"
                    }
                }
            } catch (dErr: Exception) {
                dErr.printStackTrace()
                isSuccess = false
                errorMessage = dErr.message
            }
        } else {
            isSuccess = true
        }

        // Update metadata
        val currentTime = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        settingsRepository.setLastBackupAt(currentTime)

        val fileSizeKb = excelFile.length() / 1024
        val sizeString = if (fileSizeKb > 1024) String.format(Locale.getDefault(), "%.2f MB", fileSizeKb / 1024f) else "$fileSizeKb KB"
        settingsRepository.setLastBackupSize(sizeString)

        // Save backup log to Firestore
        try {
            val log = com.laundry.stockapp.data.model.BackupLog(
                timeFormatted = currentTime,
                type = "Otomatis",
                size = sizeString,
                status = if (isSuccess) "Berhasil" else "Gagal",
                errorMessage = errorMessage,
                operator = "Sistem"
            )
            firestoreRepository.saveBackupLog(log)
        } catch (dbErr: Exception) {
            dbErr.printStackTrace()
        }
    }

    /**
     * Schedule periodic background backup task via WorkManager.
     */
    fun schedulePeriodicBackup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AutoBackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancel periodic background backup task.
     */
    fun cancelPeriodicBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("AutoBackupWork")
    }
}
