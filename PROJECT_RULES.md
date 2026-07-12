# Aturan Proyek & Konteks Bisnis (Laundry Stock App)

Dokumen ini berisi catatan penting dari percakapan dan instruksi pemilik aplikasi, yang harus selalu diingat oleh AI atau pengembang mana pun yang menyentuh kode aplikasi ini di masa depan.

## 1. Hak Akses & Pengguna (Krusial)
- **Hanya Ada Dua Pengguna:** Aplikasi ini eksklusif hanya dipakai oleh pemilik (Project Owner) dan istrinya (Admin User).
- **Kesetaraan Hak Akses:** Meskipun di dalam sistem terdapat pembedaan role `"Master App"` (Christian) dan `"Admin"` (Admin User), **keduanya memiliki hak akses yang 100% sama**.
- Istri (Admin) adalah admin kantor yang berwenang penuh untuk menambah, mengedit, dan menghapus data (stok, outlet, master item, dll).
- **DILARANG** membuat kode pembatasan hak akses berbasis `if (userRole == "Master App")` untuk fungsi-fungsi CRUD (Create, Read, Update, Delete) operasional karena keduanya memiliki wewenang setara. Sandi dan Master Password mereka disetel sama.

## 2. Kualitas UI / UX
- **Fungsionalitas Penuh:** Jangan pernah menampilkan tombol (seperti "Filter", "Urutkan", "Tambah Outlet") yang hanya berupa *mockup* kosong atau *dummy*. Jika tombol ada di UI, tombol tersebut **wajib** memiliki logika fungsi yang berjalan dengan benar dan tersambung ke *database*. Jika belum berfungsi, jangan ditampilkan.
- **Keselarasan Desain:** Jika *client* memberikan *mockup* desain antarmuka, aplikasi harus dibangun sama persis (*pixel-perfect*) dengan gambar referensi yang diberikan, termasuk pemisahan menu (misal: memisahkan layar *Backup Drive* dan *Settings*).
- **Navigasi & Layout:** Tidak boleh ada *layout* yang saling bertumpuk (hindari *hardcode* `weight` yang merusak layar). Semua halaman yang memiliki konten lebih panjang dari layar harus bisa di-*scroll* dengan menambahkan `verticalScroll(rememberScrollState())`. 

## 3. Manajemen Database & Sinkronisasi
- **Persistensi Data yang Benar:** Setiap formulir atau profil pengguna yang menyatakan "Simpan", datanya harus benar-benar ditulis ke memori permanen Android (seperti `DataStore`, `SharedPreferences`, atau `Firestore`)—bukan sekadar mengubah variabel `State` sementara di memori RAM yang akan hilang ketika halaman ditutup.
- **Mencegah Race Condition Lokal:** Karena aplikasi berjalan dengan mode *offline cache* dari Firebase (tanpa `.await()` pada fungsi `set()` agar tidak *freeze* saat tidak ada internet), perlu diperhatikan adanya jeda hitungan milidetik (*race condition*) antara proses menyimpan dan membaca ulang data. Selalu berikan sedikit jeda (`delay(200)`) setelah pemanggilan `set()` sebelum me-*refresh* data agar UI bisa menampilkan *state* terbaru dari *cache* lokal.
- **Hindari Compound Query Tanpa Indeks:** Firebase tidak mendukung pencarian beruntun (`orderBy` berganda) tanpa pengaturan *index* secara manual. Hindari penggunaan `orderBy` majemuk di sisi *database* jika indeks belum dibuat, agar tidak menyebabkan aplikasi *crash*. 

## 4. Etos Kerja & Quality Assurance (QA)
- Lakukan pengetesan seolah-olah Anda adalah *client* yang akan memakai aplikasi tersebut untuk bekerja esok paginya.
- Pastikan semua tombol, dropdown (seperti menu *Logout* di profil pojok kanan atas), dan alur data berjalan mulus.
- Segera perbaiki jika terjadi kesalahan, dan jangan berdebat tentang instruksi yang sudah sangat jelas (terutama terkait tata letak desain dan operasional bisnis).

## 5. Aturan Rilis & Kompilasi APK (Sangat Penting)
- **Peningkatan Versi Berkelanjutan**: Setiap kali mengompilasi APK rilis baru, versi aplikasi (`versionName` dan `versionCode`) **wajib** dinaikkan (misal dari v1.1.0 ke v1.1.1, dst). Jangan pernah menggunakan nomor versi yang sama untuk hasil kompilasi yang berbeda agar pengguna tidak bingung.
- **Isolasi Folder Output Proyek**: Jangan pernah mencampuradukkan file hasil kompilasi (APK, AAB) atau file laporan (.md) antar-proyek. Simpan file hasil Kiki's Laundry di folder `/Users/christambayong/Downloads/JCL KIKI/` dan Toko Queensha di folder `/Users/christambayong/Downloads/Toko Queensha/`. Jangan pernah mengompilasi atau menyalin file laundry ke folder Queensha atau sebaliknya.
- **Kehati-hatian Penghapusan File**: Sebelum menjalankan perintah penghapusan (`rm`, `delete`), periksa kembali lokasi absolut file secara detail. Pastikan tidak salah menghapus file proyek lain (misal, menghapus laporan `.md` milik Queensha saat mengelola proyek Laundry).

## 6. Tanda Tangan Digital Internal (Digital Signature)
- Selalu tambahkan signature digital di dalam kode sumber aplikasi (sebagai komentar kode atau konstanta internal): `"This App was build by Chris Tambayong - Fumakill4"`.
- Signature ini harus tertanam di dalam kodingan aplikasi, bukan untuk ditampilkan langsung di layar aplikasi (UI), sehingga jika ada yang membedah/mendekompilasi APK, signature digital tersebut akan tetap terlihat.

## 7. Mencegah Aplikasi Hang / Stuck di Latar Belakang
- **OkHttpClient Timeout**: Jangan pernah menggunakan default timeout tanpa batas waktu pada pustaka HTTP/OkHttpClient. Setel batas waktu koneksi, baca, dan tulis secara eksplisit (maksimal 15 detik).
- Jika API Google Drive tidak aktif atau koneksi internet terputus, aplikasi harus segera memberikan respon error via Toast/Dialog alih-alih macet (stuck) pada persentase tertentu (misal stuck di 80%).

## 8. Integritas Navigasi & Tombol Aksi
- Semua tautan navigasi seperti "Lihat semua", "Lihat detail transaksi terbaru", atau tombol aksi CRUD lainnya tidak boleh dibiarkan kosong `{ }`. Semuanya wajib diarahkan ke navigasi layar yang benar atau menampilkan Toast informasi jika layar tujuan belum dibuat.

