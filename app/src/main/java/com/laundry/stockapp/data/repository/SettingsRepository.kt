package com.laundry.stockapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val MASTER_PASSWORD_HASH = stringPreferencesKey("master_password_hash")
    private val GOOGLE_DRIVE_EMAIL = stringPreferencesKey("google_drive_email")
    private val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
    private val LAST_BACKUP_AT = stringPreferencesKey("last_backup_at")
    private val PROFILE_PHONE = stringPreferencesKey("profile_phone")
    private val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
    private val EXPORT_EMAIL = stringPreferencesKey("export_email")
    private val GOOGLE_DRIVE_FOLDER = stringPreferencesKey("google_drive_folder")
    private val GOOGLE_DRIVE_FOLDER_ID = stringPreferencesKey("google_drive_folder_id")
    private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    private val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    private val LAST_LOCAL_BACKUP_AT = stringPreferencesKey("last_local_backup_at")
    private val LAST_LOCAL_BACKUP_SIZE = stringPreferencesKey("last_local_backup_size")
    private val LAST_BACKUP_SIZE = stringPreferencesKey("last_backup_size")

    val masterPasswordHash: Flow<String?> = context.dataStore.data.map { it[MASTER_PASSWORD_HASH] }
    val googleDriveEmail: Flow<String?> = context.dataStore.data.map { it[GOOGLE_DRIVE_EMAIL] }
    val googleDriveFolder: Flow<String?> = context.dataStore.data.map { it[GOOGLE_DRIVE_FOLDER] }
    val googleDriveFolderId: Flow<String?> = context.dataStore.data.map { it[GOOGLE_DRIVE_FOLDER_ID] }
    val backupEnabled: Flow<Boolean> = context.dataStore.data.map { it[BACKUP_ENABLED] ?: false }
    val lastBackupAt: Flow<String?> = context.dataStore.data.map { it[LAST_BACKUP_AT] }
    val profilePhone: Flow<String?> = context.dataStore.data.map { it[PROFILE_PHONE] }
    val profileImageUri: Flow<String?> = context.dataStore.data.map { it[PROFILE_IMAGE_URI] }
    val exportEmail: Flow<String?> = context.dataStore.data.map { it[EXPORT_EMAIL] }
    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[APP_LOCK_ENABLED] ?: false }
    val appLockPin: Flow<String?> = context.dataStore.data.map { it[APP_LOCK_PIN] }
    val lastLocalBackupAt: Flow<String?> = context.dataStore.data.map { it[LAST_LOCAL_BACKUP_AT] }
    val lastLocalBackupSize: Flow<String?> = context.dataStore.data.map { it[LAST_LOCAL_BACKUP_SIZE] }
    val lastBackupSize: Flow<String?> = context.dataStore.data.map { it[LAST_BACKUP_SIZE] }

    suspend fun setMasterPassword(password: String) {
        val hash = hashString(password)
        context.dataStore.edit { preferences ->
            preferences[MASTER_PASSWORD_HASH] = hash
        }
    }

    suspend fun setMasterPasswordHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[MASTER_PASSWORD_HASH] = hash
        }
    }

    suspend fun verifyMasterPassword(password: String, currentHash: String?): Boolean {
        if (currentHash == null) return false
        return hashString(password) == currentHash
    }

    suspend fun setGoogleDriveEmail(email: String?) {
        context.dataStore.edit { preferences ->
            if (email == null) {
                preferences.remove(GOOGLE_DRIVE_EMAIL)
            } else {
                preferences[GOOGLE_DRIVE_EMAIL] = email
            }
        }
    }

    suspend fun setGoogleDriveFolder(folder: String?, folderId: String?) {
        context.dataStore.edit { preferences ->
            if (folder == null) preferences.remove(GOOGLE_DRIVE_FOLDER) else preferences[GOOGLE_DRIVE_FOLDER] = folder
            if (folderId == null) preferences.remove(GOOGLE_DRIVE_FOLDER_ID) else preferences[GOOGLE_DRIVE_FOLDER_ID] = folderId
        }
    }

    suspend fun setExportEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[EXPORT_EMAIL] = email
        }
    }

    suspend fun setProfilePhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_PHONE] = phone
        }
    }

    suspend fun setProfileImageUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_URI] = uri
        }
    }

    suspend fun setBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setLastBackupAt(timestamp: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_BACKUP_AT] = timestamp
        }
    }

    suspend fun setLastLocalBackupAt(timestamp: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LOCAL_BACKUP_AT] = timestamp
        }
    }

    suspend fun setLastLocalBackupSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LOCAL_BACKUP_SIZE] = size
        }
    }

    suspend fun setLastBackupSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_BACKUP_SIZE] = size
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAppLockPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_PIN] = pin
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
