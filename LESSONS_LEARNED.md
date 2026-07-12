# LESSONS LEARNED & CHECKLIST PEMBANGUNAN PROYEK BARU

Dokumen ini mencatat seluruh kegagalan teknis, penyebab, dan solusi konkret yang ditemukan selama pengembangan integrasi Google Drive & Firebase di Laundry Stock App, agar tidak terulang pada proyek berikutnya.

---

## 1. Masalah Jaringan & Aplikasi Hang (Infinite Loading di 80%)

* **Kegagalan**: Aplikasi membeku (*stuck/hang*) selamanya pada proses pencarian folder atau pengunggahan file tanpa melemparkan error jika jaringan terputus atau API diblokir.
* **Penyebab**: 
  * Pustaka HTTP (OkHttp) dijalankan secara sinkron (`.execute()`) di dalam Coroutine.
  * Timeout bawaan OkHttp tidak mencakup *DNS Lookup* (pencarian domain) tingkat sistem operasi. Jika DNS sistem operasi menggantung, thread Java akan terkunci selamanya dan tidak mendeteksi pembatalan Coroutine.
* **Solusi Mutlak**:
  * Ubah seluruh panggilan OkHttp menjadi **asinkron** menggunakan `enqueue` dan bungkus dengan `suspendCancellableCoroutine`.
  * Saat coroutine dibatalkan, hubungkan dengan `continuation.invokeOnCancellation { cancel() }` untuk mematikan request OkHttp secara paksa.
  * Bungkus fungsi pemanggilan API dengan batas waktu ketat menggunakan `withTimeoutOrNull(15000)` (15 detik). Jika melewati batas, lemparkan `IOException` khusus agar UI dapat dibebaskan dan menampilkan pesan error.

```kotlin
// Contoh implementasi ekstensi Call await asinkron yang aman:
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            if (!continuation.isCancelled) continuation.resume(response)
            else response.close()
        }
    })
}
```

---

## 2. Error Google Drive API: `Search folder failed: Invalid Value`

* **Kegagalan**: Server Google Drive menolak request pencarian folder dengan respon *400 Bad Request (Invalid Value)*.
* **Penyebab**: 
  * Nama folder yang dicari mengandung tanda petik tunggal (`'`), contoh: `"Kiki's Laundry Stock App"`.
  * Ketika dimasukkan ke query Google Drive (`name='Kiki's Laundry Stock App'`), karakter `'` di kata `Kiki's` dianggap menutup string pencarian lebih cepat, menyebabkan kesalahan sintaks query.
  * Parameter query `q` di URL tidak di-encode dengan benar.
* **Solusi Mutlak**:
  * Lakukan pencarian karakter `'` dan ubah menjadi karakter escape `\'` menggunakan `.replace("'", "\\'")`.
  * Selalu bungkus seluruh string parameter query `q` menggunakan `java.net.URLEncoder.encode(query, "UTF-8")` sebelum ditempelkan ke URL request.

```kotlin
val escapedFolderName = folderName.replace("'", "\\'")
val rawQuery = "name='$escapedFolderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
val encodedQuery = java.net.URLEncoder.encode(rawQuery, "UTF-8")
val queryUrl = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id)"
```

---

## 3. Error Google Drive API: `unexpected header: Content-Type`

* **Kegagalan**: Unggah multipart file ditolak oleh Google dengan pesan error `unexpected header: Content-Type`.
* **Penyebab**: 
  * Di OkHttp `MultipartBody.Builder`, kita mendefinisikan header `Content-Type` secara manual di dalam `Headers.Builder()` untuk setiap part.
  * Di saat yang sama, kita menyusun body part menggunakan `.toRequestBody(mediaType)`. OkHttp secara otomatis menulis header `Content-Type` bawaan dari `mediaType` tersebut.
  * Akibatnya, header `Content-Type` terkirim **ganda** (dua kali) di dalam satu part, yang dianggap ilegal oleh parser server Google.
* **Solusi Mutlak**:
  * Jangan pernah menambahkan header `Content-Type` secara manual menggunakan `Headers.Builder()` jika tipe media sudah didefinisikan di dalam `RequestBody`.
  * Cukup gunakan panggilan `.addPart(requestBody)` langsung agar OkHttp menulis satu header `Content-Type` yang bersih.

```kotlin
// BENAR (OkHttp menulis header Content-Type otomatis sekali saja):
val multipartBody = MultipartBody.Builder()
    .setType("multipart/related".toMediaType())
    .addPart(fileMetadata.toRequestBody(metadataMediaType))
    .addPart(file.asRequestBody(excelMediaType))
    .build()
```

---

## 4. Kegagalan Izin Google Drive (Google OAuth Consent Flow)

* **Kegagalan**: Backup gagal secara senyap saat pengguna tidak mengaktifkan persetujuan akses Google Drive pada login pertama, atau izin tersebut kedaluwarsa.
* **Penyebab**: 
  * Pengambilan token via `GoogleAuthUtil.getToken(...)` melemparkan `UserRecoverableAuthException` karena membutuhkan interaksi pengguna untuk menyetujui izin tambahan (*scopes*). Jika exception ini hanya dicatat sebagai log teks biasa, dialog persetujuan Google tidak akan pernah muncul di layar HP.
* **Solusi Mutlak**:
  * Tangkap `com.google.android.gms.auth.UserRecoverableAuthException` secara terpisah di repositori/ViewModel.
  * Ambil objek `Intent` pemulihan dari exception tersebut (`e.intent`) dan kirimkan ke UI.
  * Di sisi Compose/Activity, luncurkan intent tersebut menggunakan `rememberLauncherForActivityResult` untuk memunculkan pop-up persetujuan resmi dari Google di layar perangkat.

---

## 5. Google Cloud & Firebase Console Checklist (Wajib Sebelum Mulai Coding)

Sebelum menulis baris kode Google/Firebase di proyek baru, pastikan checklist administrasi ini sudah selesai diisi:

1. **Fingerprint SHA-1 (Debug & Release)**:
   * Daftarkan SHA-1 dari `debug.keystore` (dan keystore rilis nanti) ke dalam pengaturan aplikasi di Firebase Console.
2. **Support Email**:
   * Masuk ke Firebase Auth > Google Provider > Aktifkan dan pilih **Support email** proyek. Jika tidak diisi, server Google akan menolak login dengan *Error Code 10 (Developer Error)*.
3. **google-services.json Terbaru**:
   * Unduh ulang berkas `google-services.json` *setelah* SHA-1 terdaftar. Pastikan tag `oauth_client` di dalamnya tidak kosong (harus berisi Client ID jenis 1 dan 3).
4. **Google Drive API Status**:
   * Buka Google Cloud Console untuk proyek tersebut dan aktifkan **Google Drive API** secara manual.
5. **OAuth Consent Screen Test Users**:
   * Jika proyek dalam masa uji coba (status "Testing"), daftarkan email tester pengembang (misalnya email klien) ke daftar **Test Users** di Google Cloud Console agar Google mengizinkan otorisasi scope sensitif.
6. **Inisialisasi Firestore Database**:
   * Jangan biarkan Firestore kosong tanpa dibuat. Buat instansinya di Firebase Console dalam **"Start in Test Mode"** terlebih dahulu agar sinkronisasi data online pertama kali tidak menghasilkan error.
