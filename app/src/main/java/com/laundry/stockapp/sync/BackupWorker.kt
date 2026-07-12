package com.laundry.stockapp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.SettingsRepository
import com.laundry.stockapp.util.BackupManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupWorkerEntryPoint {
        fun firestoreRepository(): FirestoreRepository
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            BackupWorkerEntryPoint::class.java
        )
        val firestoreRepository = entryPoint.firestoreRepository()
        val settingsRepository = entryPoint.settingsRepository()

        // Check if backup is enabled in settings
        val isBackupEnabled = settingsRepository.backupEnabled.firstOrNull() ?: false
        if (!isBackupEnabled) {
            return Result.success()
        }

        return try {
            BackupManager.triggerCloudBackup(appContext, firestoreRepository, settingsRepository)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
