package com.laundry.stockapp.util

// This App was build by Chris Tambayong - Fumakill4

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GoogleDriveHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Extension function to await an OkHttp Call asynchronously and support coroutine cancellation.
     */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(e)
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isCancelled) {
                    continuation.resume(response)
                } else {
                    response.close()
                }
            }
        })
    }

    /**
     * Retrieve the OAuth 2.0 Access Token from Google Play Services.
     */
    suspend fun getAccessToken(context: Context, email: String): String = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(15000) {
            val account = Account(email, "com.google")
            val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
            GoogleAuthUtil.getToken(context, account, scope)
        }
        return@withContext result ?: throw IOException("Batas waktu (timeout) 15 detik terlampaui saat mengambil token akses Google.")
    }

    /**
     * Search for the folder 'Kiki's Laundry Stock App' in the user's Drive.
     * If not found, create it. Returns the folder's Google Drive ID.
     */
    suspend fun findOrCreateAppFolder(token: String): String = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(15000) {
            val folderName = "Kiki's Laundry Stock App"
            val escapedFolderName = folderName.replace("'", "\\'")
            val rawQuery = "name='$escapedFolderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            val encodedQuery = java.net.URLEncoder.encode(rawQuery, "UTF-8")
            
            // 1. Search for existing folder
            val queryUrl = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id)"
            
            val searchRequest = Request.Builder()
                .url(queryUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(searchRequest).await()
            response.use { res ->
                if (!res.isSuccessful) {
                    val errorBody = res.body?.string() ?: ""
                    val msg = try {
                        JSONObject(errorBody).getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        res.message
                    }
                    throw IOException("Search folder failed: $msg")
                }
                val responseBody = res.body?.string() ?: ""
                val json = JSONObject(responseBody)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return@withTimeoutOrNull files.getJSONObject(0).getString("id")
                }
            }

            // 2. Create the folder if not found
            val createUrl = "https://www.googleapis.com/drive/v3/files"
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val folderMetadata = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
            }.toString()

            val createRequest = Request.Builder()
                .url(createUrl)
                .header("Authorization", "Bearer $token")
                .post(folderMetadata.toRequestBody(jsonMediaType))
                .build()

            val createResponse = client.newCall(createRequest).await()
            createResponse.use { res ->
                if (!res.isSuccessful) {
                    val errorBody = res.body?.string() ?: ""
                    val msg = try {
                        JSONObject(errorBody).getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        res.message
                    }
                    throw IOException("Create folder failed: $msg")
                }
                val responseBody = res.body?.string() ?: ""
                val json = JSONObject(responseBody)
                return@withTimeoutOrNull json.getString("id")
            }
        }
        return@withContext result ?: throw IOException("Batas waktu (timeout) 15 detik terlampaui saat mencari/membuat folder Google Drive.")
    }

    /**
     * Upload an Excel backup file to the specified Google Drive folder.
     */
    suspend fun uploadBackupFile(token: String, folderId: String, file: File, fileName: String): Boolean = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(15000) {
            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            
            val metadataMediaType = "application/json; charset=UTF-8".toMediaType()
            val excelMediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaType()

            val fileMetadata = JSONObject().apply {
                put("name", fileName)
                put("parents", org.json.JSONArray().apply { put(folderId) })
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(fileMetadata.toRequestBody(metadataMediaType))
                .addPart(file.asRequestBody(excelMediaType))
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            val response = client.newCall(request).await()
            response.use { res ->
                if (!res.isSuccessful) {
                    val errorBody = res.body?.string() ?: ""
                    val msg = try {
                        JSONObject(errorBody).getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        res.message
                    }
                    throw IOException("Upload failed: $msg")
                }
                return@withTimeoutOrNull true
            }
        }
        return@withContext result ?: throw IOException("Batas waktu (timeout) 15 detik terlampaui saat mengunggah file ke Google Drive.")
    }
}
