// This App was build by Chris Tambayong - Fumakill4
package com.laundry.stockapp.ui.screens.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laundry.stockapp.R
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.theme.*
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
fun SettingsScreen(
    userName: String = "",
    profileImageUri: String? = null,
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

    var isAutoBackup by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("........") }
    val state by viewModel.state.collectAsState()
    var localExportEmail by remember { mutableStateOf("") }
    var isEditingEmail by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var newPasswordInput by remember { mutableStateOf("") }
    var newPasswordConfirm by remember { mutableStateOf("") }

    LaunchedEffect(state.exportEmail) {
        if (!isEditingEmail) {
            localExportEmail = state.exportEmail
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

    // Change Master Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false; newPasswordInput = ""; newPasswordConfirm = "" },
            title = {
                Text("Ubah Master Password", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF6366F1))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Password baru minimal 6 karakter", fontSize = 12.sp, color = TextGray)
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Password Baru") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPasswordConfirm,
                        onValueChange = { newPasswordConfirm = it },
                        label = { Text("Konfirmasi Password") },
                        singleLine = true,
                        isError = newPasswordConfirm.isNotBlank() && newPasswordInput != newPasswordConfirm,
                        supportingText = if (newPasswordConfirm.isNotBlank() && newPasswordInput != newPasswordConfirm) {
                            { Text("Password tidak cocok", color = Color(0xFFEF4444), fontSize = 11.sp) }
                        } else null,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newPasswordInput.length >= 6 && newPasswordInput == newPasswordConfirm,
                    onClick = {
                        viewModel.setMasterPassword(newPasswordInput)
                        showChangePasswordDialog = false
                        newPasswordInput = ""
                        newPasswordConfirm = ""
                        android.widget.Toast.makeText(context, "Master Password berhasil diubah", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) { Text("Simpan") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePasswordDialog = false; newPasswordInput = ""; newPasswordConfirm = "" }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    "Anda yakin akan melakukan reset data?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tindakan ini akan menghapus semua data transaksi, item, outlet, dan pengaturan secara permanen. Tindakan ini tidak dapat dibatalkan.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Masukan Master Password Anda untuk melakukan reset data",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Master Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData(
                            password = confirmPassword,
                            onSuccess = {
                                showResetDialog = false
                                confirmPassword = ""
                                android.widget.Toast.makeText(context, "Reset selesai", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showResetDialog = false
                        confirmPassword = ""
                    }
                ) {
                    Text("Batal")
                }
            }
        )
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
                        text = "Settings",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Kelola pengaturan aplikasi dan keamanan data",
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
            // ROW 1: Keamanan & Google Drive
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Card Keamanan
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).background(Color(0xFFF3E8FF), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Keamanan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Ubah Master Password", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0)),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, letterSpacing = 2.sp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { showChangePasswordDialog = true },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7FF)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ubah Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Gunakan password master untuk melindungi data aplikasi Anda dari akses yang tidak sah.", fontSize = 10.sp, color = TextGray, lineHeight = 14.sp)
                        }
                    }

                    // Card Google Drive
                    Card(
                        modifier = Modifier.weight(1.5f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).background(Color(0xFFF8FAFC), CircleShape), contentAlignment = Alignment.Center) {
                                    Image(painter = painterResource(id = R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Google Drive", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Akun Terhubung", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                Box(modifier = Modifier.background(Color(0xFFF0FDF4), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Terhubung", color = SecondaryTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)).padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).background(Color(0xFF10B981), CircleShape), contentAlignment = Alignment.Center) {
                                        Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(state.googleDriveEmail ?: "Belum terhubung", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (state.googleDriveEmail != null) {
                                            Text("Terhubung", fontSize = 11.sp, color = TextGray)
                                        } else {
                                            Text("Hubungkan ke Google Drive", fontSize = 11.sp, color = TextGray)
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
                                        fontSize = 12.sp, 
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Data backup Anda akan disimpan dengan aman di Google Drive.", fontSize = 10.sp, color = TextGray)
                        }
                    }
                }
            }

            // ROW 2: The complex bottom section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // LEFT COLUMN (Export & Sinkronisasi)
                    Column(modifier = Modifier.weight(1f)) {
                        // Card Export & Email
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Export & Email", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Email Default untuk Export", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = localExportEmail,
                                    onValueChange = { if (isEditingEmail) localExportEmail = it },
                                    readOnly = !isEditingEmail,
                                    trailingIcon = {
                                        if (isEditingEmail) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.setExportEmail(localExportEmail)
                                                    isEditingEmail = false
                                                    android.widget.Toast.makeText(context, "Email default disimpan", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Confirm",
                                                    tint = SecondaryTeal,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(onClick = { isEditingEmail = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = PrimaryBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedBorderColor = if (isEditingEmail) PrimaryBlue else Color(0xFFE2E8F0)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Email ini akan digunakan sebagai penerima default saat mengekspor laporan.", fontSize = 10.sp, color = TextGray, lineHeight = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Card Sinkronisasi
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).background(Color(0xFFE0F2FE), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Sinkronisasi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Status Sinkronisasi", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                    Box(modifier = Modifier.background(Color(0xFFE0F2FE), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("Tersinkron", color = Color(0xFF0284C7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Data terakhir disinkronkan", fontSize = 11.sp, color = TextDark)
                                    Text(state.lastBackupAt ?: "Belum sinkron", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                }
                                if (state.lastBackupSize != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Ukuran file backup", fontSize = 11.sp, color = TextDark)
                                        Text(state.lastBackupSize!!, fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Semua data Anda sudah sinkron dengan server cloud.", fontSize = 10.sp, color = TextGray)
                                
                                if (state.isLoading && state.syncProgress != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
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
                                            Text(progressLabel, fontSize = 10.sp, color = TextGray)
                                            Text("${state.syncProgress}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = (state.syncProgress!! / 100f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = SecondaryTeal,
                                            trackColor = Color(0xFFE2E8F0)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedButton(
                                    onClick = { viewModel.syncNow() },
                                    enabled = !state.isLoading,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryTeal),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.isLoading) "Sedang Menyinkronkan..." else "Sinkronkan Sekarang",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Card WhatsApp Bot Connection
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("WhatsApp Bot (WA JCL)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF10B981))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                if (state.waBotConnected) {
                                    Box(
                                        modifier = Modifier.size(150.dp).background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(64.dp))
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                        Text("Connected", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Bot siap menerima laporan via WhatsApp.", fontSize = 10.sp, color = TextGray)
                                } else if (!state.waBotQrCode.isNullOrEmpty()) {
                                    val encodedQr = java.net.URLEncoder.encode(state.waBotQrCode, "UTF-8")
                                    val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=$encodedQr"
                                    coil.compose.AsyncImage(
                                        model = qrUrl,
                                        contentDescription = "WhatsApp QR Code",
                                        modifier = Modifier.size(150.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                        Text("Waiting for Scan", color = Color(0xFFD97706), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Buka WhatsApp di HP Bot > Tautkan Perangkat.", fontSize = 10.sp, color = TextGray)
                                } else {
                                    Box(
                                        modifier = Modifier.size(150.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Menghubungkan ke Server WA JCL...", fontSize = 11.sp, color = TextGray)
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN (Backup Otomatis + Data Management + Log)
                    Column(modifier = Modifier.weight(2f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Card Backup Otomatis
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(36.dp).background(Color(0xFFF3E8FF), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Backup Otomatis", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                                        }
                                        Switch(
                                            checked = state.isBackupEnabled,
                                            onCheckedChange = { viewModel.setBackupEnabled(it) },
                                            modifier = Modifier.scale(0.8f),
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6366F1))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Aktifkan backup otomatis ke Google Drive secara berkala.", fontSize = 10.sp, color = TextGray)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text("Pilih Folder Backup", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                            .clickable { folderPickerLauncher.launch(null) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(state.googleDriveFolder ?: "Pilih Folder Backup Lokal", fontSize = 11.sp, color = TextDark, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Frekuensi Backup", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = "Setiap 24 Jam",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextGray) },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    // Local Auto-Backup Info
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(36.dp).background(Color(0xFFF1F5F9), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Storage, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Auto-Backup Lokal Terakhir", fontSize = 10.sp, color = TextGray)
                                                Text(state.lastLocalBackupAt ?: "Belum ada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Ukuran Lokal", fontSize = 10.sp, color = TextGray)
                                            Text(state.lastLocalBackupSize ?: "-", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Cloud Backup Info
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(36.dp).background(Color(0xFFF8FAFC), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Backup Cloud Terakhir", fontSize = 10.sp, color = TextGray)
                                                Text(state.lastBackupAt ?: "Belum ada backup", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Ukuran Cloud", fontSize = 10.sp, color = TextGray)
                                            Text(state.lastBackupSize ?: "-", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                        }
                                    }
                                }
                            }

                            // Card Data Management
                            Column(modifier = Modifier.weight(1f)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Data Management", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp)).padding(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Reset Data (Berbahaya)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFEF4444))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Tindakan ini akan menghapus semua data transaksi, item, outlet, dan pengaturan secara permanen.", fontSize = 10.sp, color = TextGray, lineHeight = 14.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { showResetDialog = true },
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Reset Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Card PIN / Kunci Aplikasi
                                var isAppLockEnabled by remember(state.isAppLockEnabled) { mutableStateOf(state.isAppLockEnabled) }
                                var appPinInput by remember(state.appLockPin) { mutableStateOf(state.appLockPin ?: "") }
                                
                                val isPinConfigured = !state.appLockPin.isNullOrEmpty()
                                var isPinEditing by remember(state.appLockPin) { mutableStateOf(state.appLockPin.isNullOrEmpty()) }
                                
                                var showVerifyOldPinDialog by remember { mutableStateOf(false) }
                                var showResetPinWithMasterPasswordDialog by remember { mutableStateOf(false) }
                                var oldPinInput by remember { mutableStateOf("") }
                                var masterPasswordInput by remember { mutableStateOf("") }
                                
                                if (showVerifyOldPinDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showVerifyOldPinDialog = false; oldPinInput = "" },
                                        title = {
                                            Text(
                                                "Masukkan PIN Lama",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = PrimaryBlue
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    "Masukkan PIN keamanan lama Anda untuk mengubah PIN.",
                                                    fontSize = 13.sp,
                                                    color = TextDark
                                                )
                                                OutlinedTextField(
                                                    value = oldPinInput,
                                                    onValueChange = { 
                                                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                            oldPinInput = it
                                                        }
                                                    },
                                                    label = { Text("PIN Lama") },
                                                    singleLine = true,
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                    ),
                                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Text(
                                                    text = "Lupa PIN? Reset dengan Master Password",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier
                                                        .clickable {
                                                            showVerifyOldPinDialog = false
                                                            showResetPinWithMasterPasswordDialog = true
                                                        }
                                                        .padding(vertical = 4.dp)
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    if (oldPinInput == state.appLockPin) {
                                                        showVerifyOldPinDialog = false
                                                        oldPinInput = ""
                                                        isPinEditing = true
                                                    } else {
                                                        android.widget.Toast.makeText(context, "PIN Lama Salah", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                            ) {
                                                Text("Verifikasi")
                                            }
                                        },
                                        dismissButton = {
                                            OutlinedButton(
                                                onClick = { showVerifyOldPinDialog = false; oldPinInput = "" }
                                            ) {
                                                Text("Batal")
                                            }
                                        }
                                    )
                                }
                                
                                if (showResetPinWithMasterPasswordDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showResetPinWithMasterPasswordDialog = false; masterPasswordInput = "" },
                                        title = {
                                            Text(
                                                "Reset PIN Keamanan",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = Color(0xFFEF4444)
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    "Masukkan Master Password Anda untuk mereset PIN keamanan.",
                                                    fontSize = 13.sp,
                                                    color = TextDark
                                                )
                                                OutlinedTextField(
                                                    value = masterPasswordInput,
                                                    onValueChange = { masterPasswordInput = it },
                                                    label = { Text("Master Password") },
                                                    singleLine = true,
                                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.resetAppPinWithMasterPassword(
                                                        password = masterPasswordInput,
                                                        onSuccess = {
                                                            showResetPinWithMasterPasswordDialog = false
                                                            masterPasswordInput = ""
                                                            isPinEditing = true
                                                            appPinInput = ""
                                                            android.widget.Toast.makeText(context, "PIN berhasil di-reset. Silakan atur PIN baru.", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { error ->
                                                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                            ) {
                                                Text("Reset PIN")
                                            }
                                        },
                                        dismissButton = {
                                            OutlinedButton(
                                                onClick = { showResetPinWithMasterPasswordDialog = false; masterPasswordInput = "" }
                                            ) {
                                                Text("Batal")
                                            }
                                        }
                                    )
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(36.dp).background(Color(0xFFE0F2FE), CircleShape), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("Kunci Aplikasi", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                                            }
                                            Switch(
                                                checked = isAppLockEnabled,
                                                onCheckedChange = { 
                                                    isAppLockEnabled = it
                                                    viewModel.setAppLockEnabled(it)
                                                },
                                                modifier = Modifier.scale(0.8f),
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Kunci aplikasi jika tidak aktif selama 30 menit.", fontSize = 10.sp, color = TextGray)
                                        
                                        if (isAppLockEnabled) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Set PIN Keamanan (6 Digit)", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = if (isPinEditing) appPinInput else "******",
                                                    onValueChange = { 
                                                        if (isPinEditing && it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                            appPinInput = it
                                                        }
                                                    },
                                                    readOnly = !isPinEditing,
                                                    placeholder = { Text("******") },
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    singleLine = true,
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                    ),
                                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                                        focusedBorderColor = if (isPinEditing) PrimaryBlue else Color(0xFFE2E8F0)
                                                    ),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { 
                                                        if (isPinEditing) {
                                                            if (appPinInput.length == 6) {
                                                                viewModel.setAppLockPin(appPinInput)
                                                                isPinEditing = false
                                                                android.widget.Toast.makeText(context, "PIN disimpan", android.widget.Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                android.widget.Toast.makeText(context, "PIN harus 6 digit", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            showVerifyOldPinDialog = true
                                                        }
                                                    },
                                                    modifier = Modifier.height(44.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                                ) {
                                                    Text(
                                                        text = if (isPinEditing) "Simpan" else "Ubah",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Card Log Aktivitas Sensitif
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ListAlt, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Log Aktivitas Sensitif (Terakhir)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                                        Text("Lihat semua", color = PrimaryBlue, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val activities = emptyList<Triple<String, String, Pair<String, String>>>()
                                
                                if (activities.isEmpty()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                                        Text("Belum ada aktivitas terbaru", color = TextGray, fontSize = 12.sp)
                                    }
                                } else {
                                    activities.forEachIndexed { index, log ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(log.first, color = TextDark, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                            Box(modifier = Modifier.weight(0.8f)) {
                                                val isSuccess = log.second == "Sukses"
                                                val bgColor = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                                val txtColor = if (isSuccess) SecondaryTeal else Color(0xFFD97706)
                                                Box(modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                    Text(log.second, color = txtColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Column(modifier = Modifier.weight(2f)) {
                                                Text(log.third.first, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(log.third.second, fontSize = 10.sp, color = TextGray)
                                            }
                                        }
                                        if (index < activities.size - 1) {
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
