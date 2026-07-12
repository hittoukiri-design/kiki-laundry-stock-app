package com.laundry.stockapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.firestore.FirebaseFirestore
import com.laundry.stockapp.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val db = FirebaseFirestore.getInstance()
    private val usersCol = db.collection("users")

    private val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
    private val LOGGED_IN_USER_ROLE = stringPreferencesKey("logged_in_user_role")
    private val LOGGED_IN_USER_NAME = stringPreferencesKey("logged_in_user_name")
    private val LOGGED_IN_USER_EMAIL = stringPreferencesKey("logged_in_user_email")

    val loggedInUserId: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_ID] }
    val loggedInUserRole: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_ROLE] }
    val loggedInUserName: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_NAME] }
    val loggedInUserEmail: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_EMAIL] }

    suspend fun login(email: String, password: String): User {
        val cleanEmail = email.trim()
        val hash = hashString(password)

        // Try to login via Firestore first to get actual doc ID and master password
        try {
            val snapshot = usersCol
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                val user = doc.toObject(User::class.java)!!

                // Allow either correct DB password hash OR hardcoded bypass password for pre-seeded users
                val isPasswordCorrect = user.passwordHash == hash ||
                        ((cleanEmail.equals("chris@tambayong.com", ignoreCase = true) ||
                          cleanEmail.equals("admin@example.com", ignoreCase = true)) && password == "12345678")

                if (isPasswordCorrect) {
                    saveSession(user.id, user.role.orEmpty(), user.name.orEmpty(), user.email.orEmpty())
                    
                    // Restore master password hash from Firestore if exists
                    val cloudHash = user.masterPasswordHash
                    if (!cloudHash.isNullOrEmpty()) {
                        settingsRepository.setMasterPasswordHash(cloudHash)
                    }
                    return user
                } else {
                    throw Exception("Email atau Sandi salah")
                }
            }
        } catch (e: Exception) {
            if (e.message == "Email atau Sandi salah") throw e
            // If network fails or user doesn't exist online, fallback to local/hardcoded check below
        }

        // Offline / cached fallback for pre-seeded users
        if (cleanEmail.equals("chris@tambayong.com", ignoreCase = true) && password == "12345678") {
            val user = User(id = "master_id", name = "Project Owner", email = cleanEmail, role = "Master App", passwordHash = "")
            saveSession(user.id, user.role.orEmpty(), user.name.orEmpty(), user.email.orEmpty())
            return user
        }
        if (cleanEmail.equals("admin@example.com", ignoreCase = true) && password == "12345678") {
            val user = User(id = "admin_id", name = "Admin User", email = cleanEmail, role = "Admin", passwordHash = "")
            saveSession(user.id, user.role.orEmpty(), user.name.orEmpty(), user.email.orEmpty())
            return user
        }

        throw Exception("Email atau Sandi salah")
    }

    suspend fun register(user: User): User {
        // Check if email exists
        val existing = usersCol.whereEqualTo("email", user.email).limit(1).get().await()
        if (!existing.isEmpty) {
            throw Exception("Email sudah terdaftar")
        }

        val docRef = usersCol.document()
        val userToSave = user.copy(
            id = docRef.id,
            passwordHash = hashString(user.passwordHash.orEmpty()) // Assume password was passed in passwordHash field
        )
        docRef.set(userToSave).await()
        
        saveSession(userToSave.id, userToSave.role.orEmpty(), userToSave.name.orEmpty(), userToSave.email.orEmpty())
        return userToSave
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.remove(LOGGED_IN_USER_ID)
            preferences.remove(LOGGED_IN_USER_ROLE)
            preferences.remove(LOGGED_IN_USER_NAME)
            preferences.remove(LOGGED_IN_USER_EMAIL)
        }
    }

    suspend fun updateMasterPasswordInCloud(password: String) {
        try {
            val userId = loggedInUserId.firstOrNull() ?: return
            val hash = hashString(password)
            if (userId != "master_id" && userId != "admin_id") {
                usersCol.document(userId).update("masterPasswordHash", hash).await()
            } else {
                // If logged in under hardcoded fallback session, update via email lookup
                val email = loggedInUserEmail.firstOrNull() ?: return
                val snapshot = usersCol.whereEqualTo("email", email).limit(1).get().await()
                if (!snapshot.isEmpty) {
                    snapshot.documents.first().reference.update("masterPasswordHash", hash).await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore exceptions to prevent crashing the offline UI flow
        }
    }

    private suspend fun saveSession(userId: String, role: String, name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[LOGGED_IN_USER_ID] = userId
            preferences[LOGGED_IN_USER_ROLE] = role
            preferences[LOGGED_IN_USER_NAME] = name
            preferences[LOGGED_IN_USER_EMAIL] = email
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
