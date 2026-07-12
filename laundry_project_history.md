# RIWAYAT PERKEMBANGAN & PERBAIKAN PROYEK LAUNDRY
## Kiki's Laundry Stock App (Android Kotlin Native)

Dokumen ini berisi catatan kronologis mengenai seluruh permasalahan teknis (bug), revisi antarmuka (UI), penyesuaian logika data, serta langkah-langkah pembaruan yang telah berhasil diimplementasikan pada aplikasi **Kiki's Laundry Stock App** dalam sesi pengerjaan ini.

---

## 1. IDENTITAS & SPESIFIKASI RILIS FINAL
* **Nama Aplikasi**: Kiki's Laundry Stock App
* **Bahasa & Framework**: Kotlin Native (Android Jetpack Compose)
* **Versi Rilis Terakhir**: **`v1.1.5.E7`** (versionCode 14)
* **Tanda Tangan Digital Internal**: `This App was build by Chris Tambayong - Fumakill4` (disisipkan pada `MainActivity.kt`, `DashboardScreen.kt`, `AboutScreen.kt`, `SettingsScreen.kt`, dan `OutletDetailScreen.kt`).
* **Lokasi Deliverable Akhir**:
  * APK Rilis: `/Users/christambayong/Downloads/Project/JCL KIKI/Apk/Kiki Laundry Stock App v.1.1.5.E7.apk`

---

## 2. KRONOLOGI MASALAH TEKNIS & SOLUSI YANG DITERAPKAN

Selama siklus pengembangan di chat ini, beberapa kendala kritis berhasil diidentifikasi dan diselesaikan secara menyeluruh:

### A. Aplikasi Menggantung (Stuck/Freeze di 80%) Saat Proses Backup
* **Masalah**: Pengguna melaporkan proses pencadangan data atau pencarian folder Google Drive berhenti total di tengah jalan tanpa memunculkan pesan error (infinite loading).
* **Penyebab**: OkHttp dijalankan secara sinkron (`.execute()`) di dalam Coroutine. Timeout bawaan tidak mencakup pencarian domain (DNS Lookup) dari sistem operasi. Jika jaringan terputus atau API Google diblokir, thread Java terkunci selamanya.
* **Solusi**:
  * Menulis ulang seluruh pemanggilan OkHttp menjadi asinkron (`.enqueue()`) yang dibungkus dengan `suspendCancellableCoroutine`.
  * Menghubungkan pembatalan coroutine dengan pembatalan request HTTP (`call.cancel()`).
  * Membungkus fungsi dengan `withTimeoutOrNull(15000)` (15 detik) untuk melempar `IOException` jika waktu habis agar UI dibebaskan dan menampilkan pesan error via Toast.

### B. Google Drive API Error: `Search folder failed: unvalid value`
* **Masalah**: Pencarian folder penyimpanan cadangan ditolak oleh server Google dengan respon *400 Bad Request*.
* **Penyebab**: Nama folder mengandung tanda petik tunggal (`'`), yaitu `"Kiki's Laundry Stock App"`. Karakter `'` memecah sintaks kueri parameter `q` pada Google Drive API, ditambah parameter tersebut belum di-URL-encode dengan aman.
* **Solusi**:
  * Melakukan manipulasi string untuk mengubah `'` menjadi `\'` menggunakan `.replace("'", "\\'")`.
  * Melakukan URL-encoding pada seluruh kueri parameter menggunakan `java.net.URLEncoder.encode(query, "UTF-8")` sebelum disematkan ke tautan URL API.

### C. Google Drive API Error: `unexpected header: Content-Type`
* **Masalah**: Proses unggah file Excel ditolak oleh server Google.
* **Penyebab**: Penggunaan builder `MultipartBody.Builder()` di mana header `"Content-Type"` didefinisikan secara manual pada `Headers.Builder()` part, bersamaan dengan `.toRequestBody(mediaType)`. Ini membuat OkHttp mengirimkan header `Content-Type` ganda dalam satu part yang dianggap ilegal oleh parser Google.
* **Solusi**: Menghapus header `"Content-Type"` manual pada builder part dan membiarkan OkHttp menyusunnya sendiri secara otomatis melalui `RequestBody`.

### D. Backup Gagal secara Senyap (Consent Google Drive)
* **Masalah**: Backup cloud gagal bagi pengguna baru karena izin akses folder Google Drive tidak pernah muncul di layar perangkat.
* **Penyebab**: Pengambilan token menggunakan `GoogleAuthUtil.getToken()` melempar `UserRecoverableAuthException` yang membutuhkan persetujuan pengguna (consent screen). Aplikasi sebelumnya hanya mencatatnya dalam log teks tanpa memicu dialog.
* **Solusi**:
  * Menangkap `UserRecoverableAuthException` di ViewModel/Repository secara terpisah.
  * Mengirimkan intent pemulihan (`recoveryIntent`) ke UI.
  * Di Compose UI, meluncurkan intent tersebut menggunakan `rememberLauncherForActivityResult` sehingga pop-up izin resmi dari Google Drive muncul di HP pengguna.

### E. Crash Deserialisasi Firestore (Null Safety)
* **Masalah**: Aplikasi mengalami crash (`NullPointerException`) saat membuka dashboard, menghapus item stok, atau memuat riwayat setelah database di-reset atau diedit manual.
* **Penyebab**: Model data Kotlin menggunakan properti non-nullable, sedangkan dokumen Firestore mengembalikan nilai null/kosong pada field tertentu.
* **Solusi**:
  * Mengubah seluruh properti data model `User`, `Item`, `Outlet`, dan `Transaction` menjadi nullable (menggunakan tanda tanya `?`).
  * Menambahkan fallback default (`?: 0`, `.orEmpty()`, `?: Date()`) saat memproses data di ViewModel dan UI Compose agar aplikasi 100% aman dari data null.

### F. Penambahan Outlet Baru CL SESETAN & Pembaruan Template Database
* **Masalah**: Menambahkan outlet baru bernama `"CL SESETAN"` di area Selatan ke dalam sistem absensi/stok dan mengganti database spreadsheet Excel template default aplikasi.
* **Penyebab**: Adanya ekspansi outlet baru yang terdaftar di spreadsheet database terbaru (`20260619`).
* **Solusi**:
  * Menyisipkan `"CL SESETAN"` ke dalam daftar seed data `seedOutlets` dan menambahkan pemeriksaan migrasi otomatis di `DataSeeder.kt` untuk memasukkan data secara real-time pada database pengguna yang sudah ada.
  * Mengganti berkas template `./app/src/main/assets/template_item_outlet.xlsx` dengan data Excel terbaru versi `20260619`.
  * Menaikkan nomor versi aplikasi menjadi **`v1.1.5.E1`** (versionCode 8) sesuai aturan penomoran edit.

### G. Perbaikan Bug Data Reset / Kehilangan Data Saat Offline
* **Masalah**: Pengguna melaporkan bahwa item stok (contoh: "clintex" dll.) atau data transaksi terkadang hilang atau ter-reset pada hari berikutnya saat aplikasi dibuka.
* **Penyebab**: Fungsi `seedDatabaseIfEmpty` memanggil `itemsCol.get().await()` tanpa opsi cache. Jika aplikasi dibuka secara offline atau koneksi lambat, kueri tersebut melemparkan pengecualian. Penanganan exception secara keliru mengasumsikan `itemCount = 0`, sehingga aplikasi menjalankan ulang proses inisialisasi basis data (seeder). Ini menyebabkan data lokal terhapus atau ditimpa dengan nilai bawaan (`startingStock = 0` dan `totalOut = 0`), yang kemudian disinkronisasikan ke Firestore saat online. Dan seeder pengguna (`seedUsers`) membuat salinan duplikat saat offline.
* **Solusi**:
  * Menulis ulang penentuan jumlah data item, outlet, dan user dengan memprioritaskan pengambilan data dari cache lokal (`Source.CACHE`) terlebih dahulu sebelum mencoba menghubungi server (`Source.SERVER`).
  * Jika kueri gagal karena masalah koneksi (menghasilkan pengecualian), status diubah menjadi `-1` (tidak diketahui), dan proses seeding basis data **dibatalkan secara aman**. Seeder hanya dieksekusi jika sistem yakin data kosong (`itemCount in 0..4`).
  * Memperbaiki `seedUsers` agar hanya menulis akun default jika kueri database berhasil dilakukan secara valid.
  * Menaikkan nomor versi aplikasi menjadi **`v1.1.5.E2`** (versionCode 9) sesuai aturan penomoran edit.

### H. Pembersihan Total Logika Auto-Deletion di Seeder
* **Masalah**: Menjamin database tidak memiliki kemampuan otomatis untuk menghapus koleksi data apa pun pada saat startup (misalnya saat mendeteksi item/outlet kurang dari 5).
* **Penyebab**: Kode seeder lama memiliki blok try-catch yang meloop `doc.reference.delete()` jika jumlah item/outlet terdeteksi kurang dari 5, yang dapat memicu penghapusan database tidak terduga di luar kendali pengguna.
* **Solusi**:
  * Menghapus total seluruh kode perulangan perintah `.delete()` pada seeder startup `DataSeeder.kt`.
  * Mengubah kriteria seeding dari rentang `0..4` menjadi `== 0` (hanya seeding saat database benar-benar kosong 0 data, seperti instalasi pertama kali).
  * Dengan perubahan ini, satu-satunya cara data dihapus secara massal adalah melalui tindakan sadar pengguna dengan mengklik tombol **"Reset Data"** di menu Settings menggunakan Master Password.
  * Versi dinaikkan menjadi **`v1.1.5.E3`** (versionCode 10) sesuai aturan penomoran edit.

---

## 3. PENYELARASAN UI/UX & TAMPILAN PREMIUM
Beberapa tata letak antarmuka dioptimalkan agar lebih proporsional dan presisi:
* **DashboardScreen**: Memperbaiki padding header (20dp), padding LazyColumn (20dp), dan jarak antar baris (12dp). Menghubungkan seluruh ringkasan kartu stok dan transaksi ke state riil database (tanpa dummy).
* **MasterItemScreen**: Menyinkronkan status badge stok (>20 = Aman, 1-20 = Perhatian, <=0 = Habis/Minus) dan menghubungkan aksi tombol edit/hapus ke master password validation.
* **OutletListScreen**: Mengubah kartu wilayah di bagian kanan atas agar menghitung jumlah outlet secara dinamis berdasarkan data Firestore wilayah tersebut.
* **ExportExcelScreen**: Menghubungkan kartu Ringkasan Isi File ke state riil data stok dan transaksi dari ViewModel.
* **Detail Riwayat Gagal**: Menghubungkan row riwayat backup dan tombol titik tiga agar memicu dialog peringatan berisi alasan log kesalahan teknis (Penyebab Gagal) secara transparan.

---

## 4. SISTEM SECURITY: CLOUD-SYNCED MASTER PASSWORD
* **Fungsionalitas**: Saat pengguna mengatur atau mengubah master password di menu **Settings**, aplikasi melakukan hash SHA-256 dan mengunggahnya secara real-time ke dokumen Firestore pengguna di tabel `users`.
* **Pemulihan Otomatis**: Jika aplikasi di-uninstall lalu di-install kembali, saat login menggunakan email terdaftar (misal `admin@example.com`), aplikasi secara otomatis mengunduh kembali hash master password dari awan ke penyimpanan Datastore lokal. Admin dapat langsung menggunakan master password lamanya secara instan tanpa perlu pengaturan ulang manual.

---

## 5. RILIS v1.1.5.E6: MENU CEKLIST & PEMELIHARAAN OUTLET (REV 2)
* **Fungsionalitas Baru**: Menambahkan menu Ceklist & Pemeliharaan terintegrasi penuh untuk memantau kelayakan mesin, regulator gas, dan masa aktif tabung APAR di setiap outlet, menghubungkan statistik riil outlet pada dashboard utama, serta memastikan seluruh data pemeliharaan ini tercakup 100% di dalam sistem pencadangan (backup) JSON dan Excel.
* **Fitur Utama & Perbaikan Baru**:
  * **Penyimpanan Backup Pemeliharaan Komprehensif (JSON & Excel)**:
    * **Backup JSON**: Memperbarui `BackupManager.kt` untuk secara aktif menarik subkoleksi ceklist mingguan, uji regulator gas, dan data APAR untuk setiap outlet dan menyimpannya di dalam snapshot JSON cadangan.
    * **Backup Excel**: Memperbarui `ExcelExporter.kt` untuk secara dinamis membuat sheet baru bernama **"Pemeliharaan & Ceklist"** di dalam file Excel laporan. Menampilkan tabel terstruktur yang memuat Nama Outlet, Wilayah, Kategori, Nama Alat, Parameter, Tanggal Terakhir Dicek, dan Tanggal Jatuh Tempo secara rapi.
  * **Sistem Toggle Ceklist (Dapat Dibatalkan)**: Pembaruan fungsi pada `checkMaintenanceItem` di `OutletViewModel` dan klik handler di `OutletDetailScreen`. Pengguna kini dapat mencentang (menyelesaikan) ceklist dan mengeklik kembali untuk membatalkan centang (mengosongkan data) dengan status tersinkron ke database dan Toast konfirmasi reaktif ("Ceklist [Nama] dibatalkan.").
  * **Statistik Riil Daftar Outlet (Bukan Dummy)**: Menghubungkan kartu outlet pada `OutletListScreen` secara dinamis dengan kueri transaksi terbaru (`state.outletStats`) dari database lokal/Firestore. Menghapus data dummy `"0"` dan menampilkan jumlah transaksi riil serta total kuantitas barang keluar untuk setiap outlet secara real-time.
  * **Sidebar Terintegrasi**: Menu navigasi baru "Ceklist & Pemeliharaan" yang menampilkan daftar outlet dengan pencarian dan filter wilayah. Mengklik nama outlet langsung membuka Tab Pemeliharaan.
  * **Tabbed UI di Detail Outlet**: Membagi layar detail outlet menjadi dua tab utama: "Riwayat Transaksi" dan "Ceklist & Pemeliharaan" (desain premium, proporsional, dan adaptif).
  * **Ceklist Pemeliharaan Mingguan**: Menyediakan 4 item bawaan (`Kipas Angin`, `Kebersihan Area Belakang Mesin`, `Rolling Door / Pintu Depan`, `Bagian Bawah Mesin`) dengan kemampuan tambah, ubah nama, hapus secara kustom.
  * **Uji Kebocoran Regulator**: Penginputan tanggal (hari dan bulan) yang sepenuhnya dapat diedit secara manual, sementara kolom tahun otomatis dikunci mengikuti jam/sistem gadget untuk kepastian akurasi tahun laporan.
  * **Pemantauan APAR (Alat Pemadam Api Ringan)**: Pelacakan tanggal pengisian tabung pemadam dan masa berlaku kustom (default 36 bulan / 3 tahun). Sistem menghitung tanggal kedaluwarsa secara dinamis dan memicu warning banner:
    * **Banner Hijau**: Masa berlaku aman (>30 hari tersisa).
    * **Banner Warning Amber**: Kurang dari 30 hari sebelum kedaluwarsa.
    * **Banner Bahaya Merah**: Sudah melewati batas tanggal.
  * **Digital Signature**: Menambahkan tanda tangan internal `"This App was build by Chris Tambayong - Fumakill4"` pada file `OutletDetailScreen.kt` yang baru.
  * **Perbaikan Tampilan Versi di Halaman Tentang (About)**: Memperbaiki string versi hardcoded lama (`1.1.1` / `1.1.5.E4`) pada kartu "Informasi Aplikasi" dan pesan Toast cek pembaruan di `AboutScreen.kt` agar otomatis sinkron menampilkan versi rilis terbaru (`v1.1.5.E5`).
  * **Kepatuhan Format Tanda Baca (Style Guide)**: Menghapus semua tanda seru (`!`) pada pesan Toast di dalam `OutletDetailScreen.kt` dan `AboutScreen.kt` serta menggantinya dengan titik (`.`) untuk kepatuhan tata bahasa yang profesional.

---

## 6. RILIS v1.1.5.E7: INTEGRASI TANGGUH CEKLIST KE SHEET OUTLET & REASSURANCE SIMPAN
* **Fungsionalitas Baru**:
  * **Integrasi Ceklist & Pemeliharaan ke Kolom I-N Tiap Sheet Outlet**: Data ceklist mingguan, uji regulator gas, dan pemantauan APAR kini disinkronisasikan dan ditulis langsung ke dalam **kolom I, J, K, L, M, dan N** di dalam **masing-masing sheet outlet yang bersangkutan** (seperti JCL BYPASS, CL SESETAN, dll.). Ini memanfaatkan area kosong di sebelah kanan tabel transaksi dan merangkum seluruh informasi operasional outlet dalam satu tampilan sheet yang padu.
  * **Penyempurnaan Antarmuka Excel**: Saat pengguna mengklik nama outlet di sheet Beranda, sheet outlet yang bersangkutan akan terbuka dan menampilkan riwayat transaksi di sebelah kiri (kolom A-D) serta status pemeliharaan terbaru di sebelah kanan (kolom I-N) secara berdampingan. Sheet terpisah "Pemeliharaan & Ceklist" di bagian paling belakang telah dihapus untuk efisiensi navigasi berkas.
  * **Tombol Simpan & Selesai**: Menambahkan tombol hijau premium (`SafeGreen`) di bagian paling bawah tab Ceklist & Pemeliharaan, memicu proses simpan visual selama 300ms (menampilkan loading spinner), memunculkan Toast sukses ("Semua data pemeliharaan berhasil disimpan secara aman."), lalu otomatis menavigasi kembali ke layar sebelumnya.
  * **Kartu Status Penyimpanan Otomatis**: Menyisipkan kartu status berwarna hijau lembut dengan ikon `CloudDone` di atas tombol Simpan untuk menyatakan bahwa penyimpanan otomatis aktif secara real-time.
  * **Penghapusan Tanda Seru**: Menyelaraskan seluruh teks peringatan APAR dan Toast agar tidak menggunakan tanda seru (`!`), melainkan diakhiri dengan tanda titik (`.`).
  * **Kompilasi v1.1.5.E7 (versionCode 14)**: Berhasil melakukan build dan kompilasi versi terbaru dengan integrasi data penuh.

---
*Catatan: Seluruh riwayat perubahan di atas telah diuji secara menyeluruh melalui instalasi bersih (clean install) dan kompilasi APK rilis berjalan 100% sukses.*

