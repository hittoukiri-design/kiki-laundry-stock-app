package com.laundry.stockapp.ui.screens.backup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.laundry.stockapp.R
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.theme.*

import androidx.hilt.navigation.compose.hiltViewModel
import com.laundry.stockapp.ui.screens.settings.SettingsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupDriveScreen(
    userName: String = "",
    profileImageUri: String? = null,
    onNavigateHistory: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = result.data?.let { GoogleSignIn.getSignedInAccountFromIntent(it) }
        if (task != null) {
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email
                if (email != null) {
                    viewModel.connectToGoogleDrive(email)
                    Toast.makeText(context, "Google Drive terhubung: $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal mendapatkan detail akun Google", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                val statusCode = e.statusCode
                val statusMessage = when (statusCode) {
                    10 -> "Developer Error (SHA-1 belum terdaftar di Firebase/Google Cloud)"
                    7 -> "Network Error (Cek koneksi internet)"
                    12500 -> "Sign in failed (12500 - Konfigurasi OAuth bermasalah)"
                    else -> e.message ?: "Unknown error"
                }
                Toast.makeText(context, "Koneksi gagal: $statusMessage (Code: $statusCode)", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Koneksi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Koneksi dibatalkan/gagal (Result Code: ${result.resultCode})", Toast.LENGTH_SHORT).show()
        }
    }

    var isAutoBackupEnabled by remember { mutableStateOf(true) }
    val state by viewModel.state.collectAsState()
    var selectedLogForDetail by remember { mutableStateOf<com.laundry.stockapp.data.model.BackupLog?>(null) }

    val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
            val folderName = documentFile?.name ?: "Pilihan Folder"
            viewModel.setGoogleDriveFolder(folderName, uri.toString())
            android.widget.Toast.makeText(context, "Folder terpilih: $folderName", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val recoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.syncNow()
        }
        viewModel.clearRecoveryIntent()
    }

    LaunchedEffect(state.recoveryIntent) {
        state.recoveryIntent?.let {
            recoveryLauncher.launch(it)
        }
    }

    LaunchedEffect(state.exportSuccessMessage) {
        state.exportSuccessMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearExportSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Match background
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Backup Drive",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Kelola dan pantau backup data ke Google Drive dengan aman dan mudah",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }
                TopProfileBar(
                    userName = userName,
                    profileImageUri = profileImageUri
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 8.dp)
        ) {
            // Row 1: Google Account & Folder Info
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Card 1: Akun Google Terhubung
                    Card(
                        modifier = Modifier.weight(1f).height(85.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color(0xFFF8FAFC), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(painter = painterResource(id = R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Akun Google Terhubung", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(state.googleDriveEmail ?: "Belum terhubung", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (state.googleDriveEmail != null) {
                                        Text("Google Drive siap digunakan", fontSize = 10.sp, color = TextGray)
                                    } else {
                                        Text("Hubungkan ke Google Drive", fontSize = 10.sp, color = TextGray)
                                    }
                                }
                            }
                            val isConnected = state.googleDriveEmail != null
                            val isConnecting = state.isGoogleDriveConnecting
                            OutlinedButton(
                                onClick = {
                                    if (!isConnecting) {
                                        if (isConnected) {
                                            googleSignInClient.signOut().addOnCompleteListener {
                                                viewModel.disconnectGoogleDrive()
                                                Toast.makeText(context, "Akun diputuskan", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                        }
                                    }
                                },
                                enabled = !isConnecting,
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isConnecting) Color.Gray else if (isConnected) Color(0xFFFECACA) else PrimaryBlue
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isConnecting) Color.Gray else if (isConnected) Color(0xFFEF4444) else PrimaryBlue
                                )
                            ) {
                                Text(
                                    text = if (isConnecting) "Menghubungkan..." else if (isConnected) "Putuskan" else "Hubungkan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Card 2: Folder Backup
                    Card(
                        modifier = Modifier.weight(1f).height(85.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Folder Backup di Penyimpanan Lokal", fontSize = 11.sp, color = SecondaryTeal, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(state.googleDriveFolder ?: "Belum ada folder terpilih", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (state.googleDriveFolderId != null) {
                                        Text("Izin akses penyimpanan aktif", fontSize = 10.sp, color = TextGray)
                                    } else {
                                        Text("Pilih folder untuk menaruh hasil backup", fontSize = 10.sp, color = TextGray)
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih Folder", color = PrimaryBlue, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Row 2: Backup Otomatis & Backup Sekarang
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color(0xFFF3E8FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Backup Otomatis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Aktifkan untuk mencadangkan data secara otomatis ke Google Drive", fontSize = 12.sp, color = TextGray)
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = state.isBackupEnabled,
                                    onCheckedChange = { viewModel.setBackupEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF6366F1),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (state.isBackupEnabled) "Aktif" else "Nonaktif", color = if (state.isBackupEnabled) Color(0xFF6366F1) else TextGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                Spacer(modifier = Modifier.width(32.dp))
                                
                                var expanded by remember { mutableStateOf(false) }
                                var frekuensi by remember { mutableStateOf("Harian") }
                                
                                Box {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Text("Frekuensi: $frekuensi", color = TextDark, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextDark, modifier = Modifier.size(14.dp))
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(text = { Text("Harian", fontSize = 11.sp) }, onClick = { frekuensi = "Harian"; expanded = false })
                                        DropdownMenuItem(text = { Text("Mingguan", fontSize = 11.sp) }, onClick = { frekuensi = "Mingguan"; expanded = false })
                                        DropdownMenuItem(text = { Text("Bulanan", fontSize = 11.sp) }, onClick = { frekuensi = "Bulanan"; expanded = false })
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(32.dp))
                                
                                val isSyncing = state.syncProgress != null || state.isLoading
                                Button(
                                    onClick = {
                                        if (!isSyncing) {
                                            viewModel.syncNow()
                                            android.widget.Toast.makeText(context, "Backup dimulai...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isSyncing,
                                    modifier = Modifier.width(220.dp).height(54.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSyncing) Color.Gray else Color(0xFF6366F1)
                                    )
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = if (isSyncing) "Mengunggah..." else "Backup Sekarang", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isSyncing) "Harap tunggu..." else "Jalankan backup manual", 
                                            fontSize = 10.sp, 
                                            color = Color.White.copy(alpha=0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        if (state.isLoading && state.syncProgress != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val progressLabel = when {
                                        state.syncProgress!! < 30 -> "Menghubungkan ke server..."
                                        state.syncProgress!! < 60 -> "Mengumpulkan data transaksi..."
                                        state.syncProgress!! < 70 -> "Membuat file Excel..."
                                        state.syncProgress!! < 90 -> "Mengunggah backup ke Google Drive..."
                                        else -> "Hampir selesai..."
                                    }
                                    Text(progressLabel, fontSize = 11.sp, color = TextGray)
                                    Text("${state.syncProgress}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = (state.syncProgress!! / 100f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF6366F1),
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }

            // Row 3: Four Small Stats Cards
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Card 1
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Backup Terakhir", fontSize = 10.sp, color = TextGray)
                                Text(state.lastBackupAt ?: "Belum ada backup", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Oleh: ${if (state.lastBackupAt != null) userName else "-"}", fontSize = 9.sp, color = TextGray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (state.lastBackupAt != null) "Berhasil (${state.lastBackupSize ?: "-"})" else "-", fontSize = 9.sp, color = if (state.lastBackupAt != null) SecondaryTeal else TextGray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    // Card 2
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFF3E8FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Menunggu Sinkronisasi", fontSize = 10.sp, color = TextGray)
                                Text("0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("Semua data tersinkron", fontSize = 9.sp, color = TextGray)
                            }
                        }
                    }

                    // Card 3
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Status Internet", fontSize = 10.sp, color = TextGray)
                                Text("Online", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Text("Koneksi stabil", fontSize = 9.sp, color = SecondaryTeal, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Card 4
                    Card(
                        modifier = Modifier.weight(1.2f).height(80.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Jenis File yang Dibackup", fontSize = 10.sp, color = TextGray)
                                Text("JSON & Excel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("JSON terenkripsi, Excel terbaru", fontSize = 9.sp, color = TextGray)
                            }
                        }
                    }
                }
            }

            // Row 4: History Table and Info Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Left Column: Table
                    Column(modifier = Modifier.weight(1.8f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Riwayat Backup Terbaru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNavigateHistory() }) {
                                Text("Lihat semua", color = PrimaryBlue, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(PrimaryBlue).padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tanggal & Waktu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                    Text("Jenis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Ukuran", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Oleh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(24.dp)) // For trailing icon
                                }
                                
                                val history = state.backupHistory
                                if (history.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Belum ada riwayat backup terbaru", color = TextGray, fontSize = 12.sp)
                                    }
                                } else {
                                    history.take(5).forEachIndexed { index, log ->
                                        val user = log.operator ?: "Sistem"
                                        val isSuccess = log.status == "Berhasil"
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedLogForDetail = log }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(log.timeFormatted.orEmpty(), color = TextDark, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                            Text(log.type.orEmpty(), color = TextDark, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                            Text(log.size.orEmpty(), color = TextGray, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                                    contentDescription = null,
                                                    tint = if (isSuccess) SecondaryTeal else Color(0xFFEF4444),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = log.status.orEmpty(),
                                                    color = if (isSuccess) SecondaryTeal else Color(0xFFEF4444),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(user, color = TextGray, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = { selectedLogForDetail = log },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Detail", tint = TextGray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (index < history.take(5).size - 1) {
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Info
                    Column(modifier = Modifier.weight(1f)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Informasi Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Aplikasi ini secara otomatis melakukan backup data penting ke Google Drive Anda.",
                                    fontSize = 12.sp,
                                    color = TextGray,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Row {
                                    Box(modifier = Modifier.size(32.dp).background(Color(0xFFF3E8FF), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("JSON terenkripsi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6366F1))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Data transaksi dan master item dalam format JSON dienkripsi untuk keamanan ekstra.", fontSize = 11.sp, color = TextGray, lineHeight = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row {
                                    Box(modifier = Modifier.size(32.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.TableView, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("File Excel terbaru", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SecondaryTeal)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Laporan dan data export Excel terbaru akan selalu dicadangkan secara otomatis.", fontSize = 11.sp, color = TextGray, lineHeight = 14.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)).padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Pastikan akun Google Drive Anda memiliki ruang penyimpanan yang cukup untuk proses backup.",
                                        fontSize = 11.sp,
                                        color = PrimaryBlue,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Detail Dialog
    if (selectedLogForDetail != null) {
        val log = selectedLogForDetail!!
        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            title = {
                Text(
                    text = "Detail Riwayat Backup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryBlue
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Tanggal & Waktu: ", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1.2f))
                        Text(log.timeFormatted.orEmpty(), color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(2f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Jenis Backup: ", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1.2f))
                        Text(log.type.orEmpty(), color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(2f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Ukuran File: ", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1.2f))
                        Text(log.size.orEmpty(), color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(2f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Operator: ", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1.2f))
                        Text(log.operator.orEmpty(), color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(2f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Status: ", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1.2f))
                        val isSuccess = log.status == "Berhasil"
                        Text(
                            text = log.status.orEmpty(),
                            color = if (isSuccess) SecondaryTeal else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(2f)
                        )
                    }
                    if (log.status == "Gagal" && !log.errorMessage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Penyebab Gagal:", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(log.errorMessage!!, color = Color(0xFFB91C1C), fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedLogForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup", color = Color.White)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}
