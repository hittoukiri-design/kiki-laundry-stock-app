# PROJECT CHECKLIST & DEVELOPMENT GUIDELINES
## Panduan Mutlak Pembangunan Aplikasi Bebas Error & Aman (Zero-Fault Standard)

Dokumen ini adalah **panduan utama, kompilasi aturan, riwayat kesalahan teknis (bug), revisi, dan instruksi penanganan data** yang dikompilasi dari proyek sebelumnya. 

> [!IMPORTANT]
> **PETUNJUK UNTUK AI AGENT BARU:** 
> Sebelum Anda menulis baris kode pertama pada proyek baru ini, **baca seluruh dokumen ini sampai habis**. Anda wajib mematuhi setiap aturan, arsitektur penanganan jaringan, alur autentikasi, serta standar UI/UX yang tercantum di sini untuk mencegah crash, hilangnya data pengguna, atau aplikasi membeku (hang).

---

## 1. ATURAN EKSEKUSI MUTLAK (USER RULES)

Setiap kali ada instruksi untuk memperbaiki (*fix*), memperbarui, atau merevisi fitur aplikasi:

1. **Fokus pada Cakupan (Scope Compliance)**:
   * **Cukup kerjakan apa yang diminta oleh user.** Jangan pernah merubah kode, tata letak UI, atau logika lain yang tidak ada kaitannya dengan revisi tersebut.
   * Perubahan di luar cakupan hanya diperbolehkan jika revisi tersebut memang berdampak langsung secara sistem pada menu/behavior lain.
   * *Alasan*: Menghindari regresi di mana perbaikan fitur A merusak fitur B yang sebelumnya berjalan normal.

2. **Tanpa Ketololan Teknis (Zero Stub / Half-Baked Solutions)**:
   * Semua tombol aksi (Simpan, Edit, Hapus, Cadangkan) dan navigasi (seperti teks klik "Lihat semua") wajib dihubungkan ke fungsi yang berjalan (*functional handler*).
   * **Dilarang keras** membiarkan event handler kosong seperti `{ }` pada Compose/React Native. Jika fungsionalitas belum siap, minimal tampilkan pesan `Toast` informatif yang menunjukkan bahwa fitur sedang diakses.
   * Lakukan kompilasi bersih dan pastikan **0 error/warnings penting** sebelum menyerahkan hasil build (APK/AAB) kepada user.

3. **Aturan Penomoran Versi Edit (Incremental Editing Versioning)**:
   * Setiap kali ada revisi, perubahan, atau perbaikan antarmuka/tombol/penulisan di dalam kode selama fase uji coba, nomor versi aplikasi wajib menggunakan akhiran suffix **`.EX`** (di mana `X` adalah urutan edit ke-X).
   * **Alur Penomoran Versi**:
     * Versi dasar awal: **`V.1.0.0`**
     * Edit/perbaikan pertama: **`V.1.0.0.E1`**
     * Edit/perbaikan kedua: **`V.1.0.0.E2`** (dan seterusnya)
     * Setelah pengguna puas dan menyetujui build tersebut, compile menjadi versi bersih berikutnya: **`V.1.1.0`**
     * Jika versi bersih **`V.1.1.0`** kembali diedit karena ada revisi baru, penomoran berubah menjadi **`V.1.1.0.E1`**, kemudian **`V.1.1.0.E2`**, dst.
   * Terapkan penamaan versi ini baik pada konfigurasi gradle (`versionName`) maupun pada tampilan layar Tentang/About aplikasi.

4. **Tanda Tangan Digital Internal (In-Code Digital Signature)**:
   * Selalu sisipkan komentar tanda tangan digital berikut ke dalam file kode sumber utama (seperti `MainActivity.kt`, `DashboardScreen.kt`, dsb.) sebagai komentar internal:
     ```kotlin
     // This App was build by Chris Tambayong - Fumakill4
     ```
   * *Ketentuan*: Tanda tangan ini diletakkan di dalam baris kode (code comment atau internal metadata constant), **bukan pada antarmuka pengguna (UI)** yang terlihat oleh user biasa. Tujuannya adalah agar tanda tangan ini tetap terbaca ketika APK didekompilasi.

5. **Isolasi Folder Output & File Proyek**:
   * **Folder Induk Project**: `/Users/christambayong/Downloads/Project/`
   * **Berkas Checklist Master**: File `project_check_list.md` wajib disimpan langsung di bawah folder induk: `/Users/christambayong/Downloads/Project/project_check_list.md`.
   * **Folder Spesifik Proyek**: Setiap aplikasi baru wajib dibuatkan foldernya sendiri di dalam folder induk menggunakan nama proyek tersebut, misalnya:
     * Aplikasi Absensi & Gaji: `/Users/christambayong/Downloads/Project/Aplikasi Gaji/`
     * Aplikasi Laundry Kiki: `/Users/christambayong/Downloads/Project/JCL KIKI/`
     * Aplikasi Toko Queensha: `/Users/christambayong/Downloads/Project/Toko Queensha/`
   * **Penyimpanan Berkas Proyek**: Semua perubahan kode, APK/AAB rilis, dan file laporan `.md` yang spesifik untuk proyek tersebut **harus** diletakkan di dalam folder proyek masing-masing.
   * *Tujuan*: Agar pengguna dapat menyuruh AI Agent memanggil (*recall*) file `.md` spesifik langsung dari folder proyek tersebut, serta membaca aturan master dari folder induk `Project/`.

---

## 2. PRE-REQUISITES: ADMINISTRASI GOOGLE DRIVE & FIREBASE CONSOLE
*(Wajib diselesaikan sebelum menulis kode integrasi)*

Sebelum mengaktifkan sinkronisasi Google Drive atau Firebase di aplikasi baru, verifikasi checklist server-side berikut bersama user:

| No | Langkah Konfigurasi | Dampak Jika Terlewat / Salah |
|---|---|---|
| **1** | **Daftarkan Fingerprint SHA-1 (Debug & Release)** ke Firebase Console. | Google Sign-In gagal dengan **Error Code 12500** atau **8 (Internal Error)**. |
| **2** | **Konfigurasi OAuth Support Email** di Firebase Auth > Google Provider. | Login Google diblokir oleh server dengan **Error Code 10 (Developer Error)**. |
| **3** | **Unduh `google-services.json` Terbaru** setelah SHA-1 didaftarkan dan pastikan array `oauth_client` terisi Client ID tipe 1 dan 3. | Autentikasi Google gagal secara instan saat inisialisasi SDK. |
| **4** | **Aktifkan Google Drive API** secara manual di Google Cloud Console untuk proyek yang aktif. | Request folder atau file cadangan ditolak dengan error `403 Forbidden` (API not enabled). |
| **5** | **Ubah Status OAuth Consent Screen ke "Testing"** dan daftarkan email pengguna/tester ke daftar **"Test Users"**. | Pengguna tidak bisa login Google atau memberikan consent karena aplikasi dinilai belum terverifikasi oleh Google. |
| **6** | **Inisialisasi Database Firestore** (mulai dalam "Test Mode" jika awal proyek). | Sinkronisasi data online pertama kali menghasilkan error permission-denied dan gagal menyimpan data. |

---

## 3. RIWAYAT KEGAGALAN TEKNIS & SOLUSI MUTLAK (BUG RECOVERY ENGINE)

Berikut adalah daftar kesalahan fatal yang terjadi pada proyek lama beserta solusi arsitektur yang wajib diterapkan di proyek baru:

### A. Jaringan Terputus / API Diblokir Menyebabkan Aplikasi Beku (Hang di 80%)
* **Deskripsi Kesalahan**: Saat melakukan backup otomatis atau pencarian folder, indikator kemajuan (progress bar) membeku selamanya tanpa menampilkan pesan error.
* **Penyebab**: 
  1. Penggunaan fungsi sinkron `.execute()` milik OkHttpClient di dalam Coroutine.
  2. Batas waktu (*timeout*) bawaan dari OkHttp tidak mencakup *DNS Lookup* tingkat OS. Jika DNS menggantung, thread Java terkunci selamanya dan mengabaikan pembatalan Coroutine.
* **Solusi Mutlak**:
  * Ubah seluruh call OkHttp menjadi asinkron dengan membungkus `enqueue` menggunakan `suspendCancellableCoroutine`.
  * Pastikan untuk menghentikan request secara fisik jika Coroutine dibatalkan dengan memanggil `call.cancel()` di dalam listener pembatalan.
  * Bungkus fungsi pemanggilan API luar dengan `withTimeoutOrNull(15000)` (15 detik) untuk melempar `IOException` jika waktu habis.

```kotlin
// Ekstensi OkHttp Call asinkron yang aman dari hang/blocking:
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { 
        // Batalkan request HTTP secara fisik jika coroutine dibatalkan
        cancel() 
    }
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

### B. Google Drive API Error: `Search folder failed: Invalid Value (400)`
* **Deskripsi Kesalahan**: Query pencarian folder Google Drive gagal total dan mengembalikan respon BadRequest.
* **Penyebab**: 
  Nama folder mengandung tanda petik tunggal (`'`), misalnya `"Kiki's Laundry Stock App"`. Saat dimasukkan ke query string Google Drive `name='Kiki's Laundry Stock App'`, tanda petik di kata `Kiki's` memecah sintaks query. Selain itu, parameter tidak di-URL-encode dengan benar.
* **Solusi Mutlak**:
  1. Selalu escape tanda petik tunggal (`'`) menjadi (`\'`) menggunakan `.replace("'", "\\'")`.
  2. Selalu gunakan `java.net.URLEncoder.encode(query, "UTF-8")` untuk mengodekan parameter query sebelum digabungkan ke URL.

---

### C. Google Drive API Error: `unexpected header: Content-Type (400)`
* **Deskripsi Kesalahan**: Proses unggah file Excel/JSON ditolak oleh Google Drive API karena masalah header.
* **Penyebab**: 
  Di dalam `MultipartBody.Builder()` OkHttp, header `"Content-Type"` ditulis secara manual pada `Headers.Builder()` untuk part metadata atau file. Di saat bersamaan, kita menggunakan `.toRequestBody(mediaType)`. Ini membuat OkHttp mengirimkan header `Content-Type` secara ganda, yang dianggap ilegal oleh Google.
* **Solusi Mutlak**:
  Jangan tulis `"Content-Type"` secara manual di `Headers.Builder()` jika sudah menggunakan `RequestBody` yang memiliki tipe media. Cukup biarkan OkHttp menyusunnya sendiri.

---

### D. Gagal Izin Google Drive (Google OAuth Consent Flow Terlewat)
* **Deskripsi Kesalahan**: Backup otomatis gagal secara senyap saat pengguna tidak mengaktifkan persetujuan akses Google Drive pada login pertama, atau izin tersebut kedaluwarsa.
* **Penyebab**: 
  Pengambilan token via `GoogleAuthUtil.getToken(...)` melempar `UserRecoverableAuthException` karena membutuhkan interaksi pengguna untuk menyetujui izin tambahan (*scopes*). Jika exception ini hanya dicatat sebagai log teks biasa, dialog persetujuan Google tidak akan pernah muncul di layar HP.
* **Solusi Mutlak**:
  * Tangkap `com.google.android.gms.auth.UserRecoverableAuthException` secara terpisah di repositori/ViewModel.
  * Ambil objek `Intent` pemulihan dari exception tersebut (`e.intent`) dan kirimkan ke UI.
  * Di sisi Compose/Activity, luncurkan intent tersebut menggunakan `rememberLauncherForActivityResult` untuk memunculkan pop-up persetujuan resmi dari Google di layar perangkat.

---

### E. Null Safety & Crash Deserialisasi Data Kosong / Null Dari Database
* **Deskripsi Kesalahan**: Aplikasi mengalami `NullPointerException` atau crash seketika saat memuat dashboard, daftar item, atau riwayat transaksi setelah data lama dihapus atau diedit secara manual.
* **Penyebab**: 
  Model data didefinisikan dengan properti non-nullable (misal `val nama: String` bukan `String?`), sementara database (seperti Firestore) mengembalikan dokumen kosong, tidak lengkap, atau bernilai null pada field tersebut.
* **Solusi Mutlak**:
  1. Definisikan semua properti di kelas model data (Entity/DTO) sebagai nullable (`String?`, `Int?`, `Double?`, `Date?`).
  2. Berikan nilai fallback (*non-null default values*) saat data diproses di ViewModel atau saat dirender di antarmuka UI (Compose/React Native) menggunakan operator Elvis `?:` atau `.orEmpty()`.

---

### F. Kehilangan Data / Data Reset Akibat Kueri Seeder Offline
* **Deskripsi Kesalahan**: Pada startup aplikasi, data item atau user yang telah ditambahkan atau diedit sebelumnya tiba-tiba hilang/ter-reset ke 0 atau terduplikasi.
* **Penyebab**: Fungsi seeder (`seedDatabaseIfEmpty`) mengecek keberadaan data menggunakan kueri jaringan biasa (tanpa CACHE). Jika pengguna membuka aplikasi dalam keadaan offline (tidak ada sinyal), kueri tersebut gagal dan melemparkan exception. Penanganan catch yang tidak aman membiarkan variabel jumlah data tetap 0 (asumsi kosong), sehingga sistem memicu proses seeding ulang. Ini menghapus data cache lokal dan menulis ulang item default dengan stok awal 0, yang kemudian disinkronkan ke server saat online (menyebabkan data hilang). Proses seed user juga menduplikasi akun karena query offline gagal mendeteksi email yang sudah ada.
* **Solusi Mutlak**:
  * Selalu fetch data menggunakan cache-first pattern (`Source.CACHE`) untuk pengecekan keberadaan data lokal saat startup.
  * Inisialisasi variabel pengecek dengan nilai default `-1` (bukan 0).
  * Jika kueri database gagal atau melempar exception (misal karena offline), biarkan nilainya tetap `-1` dan **dilarang keras menjalankan fungsi seeder/penghapusan**. Proses seeding hanya dijalankan jika sistem mengonfirmasi secara valid bahwa data kosong (jumlah data `in 0..4`).
  * Tambahkan validasi sukses kueri sebelum membuat user baru di database.

---

## 4. PEDOMAN UI/UX & TATA LETAK ELEMEN (ANTI-AI TOLOL STYLE)

Aplikasi premium wajib dirancang secara proporsional dan presisi. Hindari elemen desain standar AI generator yang tidak rapi:

1. **Tata Letak Proporsional & Spacing Wajar**:
   * **Dilarang keras** menggunakan kartu-kartu (cards) raksasa yang memakan ruang layar secara berlebihan.
   * **Dilarang keras** menggunakan tombol-tombol berukuran super besar (*oversized buttons*) yang tidak estetis dan tidak proporsional dengan komponen lain.
   * Terapkan jarak antar elemen (*spacing* dan *padding*) secara wajar dan seimbang (hindari spacing brutal yang terlalu renggang). Gunakan standar 8dp, 12dp, atau 16dp.

2. **Keutuhan Kata & Tulisan (No Text Cut-offs / Weird Hyphenation)**:
   * Seluruh teks harus terbaca secara utuh. **Dilarang keras** membiarkan kata-kata penting terpotong atau terpenggal di tengah secara aneh akibat kolom yang terlalu sempit atau penataan kata yang buruk (contoh: kata `"berlangganan"` terpenggal menjadi `"berlangga-nan"`).
   * **Solusi Teknis**:
     * Gunakan setelan ukuran font yang proporsional.
     * Batasi kata penting agar tidak membungkus baris jika tidak diperlukan, atau gunakan `maxLines = 1` dengan `overflow = TextOverflow.Ellipsis`.
     * Gunakan alokasi lebar kolom dinamis (`weight` atau `width(IntrinsicSize.Max)`) untuk memastikan kolom teks memiliki ruang yang cukup.

3. **Detail Riwayat Kegagalan**:
   * Jika cadangan (backup) atau sinkronisasi gagal, riwayat cadangan harus dapat diklik (row item atau tombol tiga titik) untuk menampilkan dialog detail.
   * Dialog harus menampilkan detail teknis error secara transparan di bawah kolom **"Penyebab Gagal"** (misalnya: *"Penyebab Gagal: Waktu koneksi ke Google Drive habis"*).

4. **Indikator Kemajuan Progresif (Linear Progress Indicator)**:
   * Gunakan `LinearProgressIndicator` dengan sudut membulat (*rounded clip*) lengkap dengan pesan status langkah aktif (contoh: *"Menghubungkan ke server..."*) dan persentase progres numerik.

5. **Informasi Ukuran File Nyata (Dynamic File Size)**:
   * Setiap catatan riwayat pencadangan lokal atau cloud **wajib** mencantumkan ukuran file riil (dalam KB atau MB, contoh: `142 KB`) di samping timestamp waktu sukses. Jangan gunakan angka dummy.

---

## 5. ALUR VALIDASI SEBELUM RILIS (FINAL CHECKS)

Sebelum menyatakan tugas selesai dan menyerahkan aplikasi kepada user:

1. **Lakukan Instalasi Bersih (Clean Install Test)**:
   * Hapus aplikasi lama di perangkat uji, lalu instal file APK baru.
   * Uji alur masuk (*login flow*) pertama kali untuk memastikan data master password terunduh kembali dari Firestore (`masterPasswordHash`) dan autentikasi berjalan mulus.
2. **Uji Kasus Ekstrim (Edge Case Verification)**:
   * Coba hapus item stok secara acak untuk memverifikasi null safety.
   * Matikan internet di tengah-tengah sinkronisasi dan pastikan aplikasi tidak hang (harus memunculkan Toast gagal dalam waktu maksimal 15 detik).
3. **Pindahkan File Rilis Secara Akurat**:
   * Kompilasi APK rilis dengan nama file dan nomor versi edit yang tepat.
   * Salin file APK ke folder tujuan proyek yang tepat:
     * Lokasi: `/Users/christambayong/Downloads/Project/[Nama Proyek]/`

---
*Dokumen ini disusun agar tidak ada lagi kesalahan dasar yang terulang. Patuhi setiap poin demi keamanan database dan reputasi aplikasi.*
