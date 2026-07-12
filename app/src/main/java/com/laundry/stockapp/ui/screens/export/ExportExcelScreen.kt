package com.laundry.stockapp.ui.screens.export

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
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportExcelScreen(
    userName: String = "",
    profileImageUri: String? = null,
    viewModel: ExportExcelViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    var fileName by remember { mutableStateOf("Laporan_Stok.xlsx") }
    var selectedScope by remember { mutableStateOf("Semua Data") }
    var selectedDestination by remember { mutableStateOf("Perangkat ini") }
    var email by remember { mutableStateOf("") }
    LaunchedEffect(state.defaultEmail) {
        email = state.defaultEmail
    }

    val scopeOptions = listOf(
        Triple("Semua Data", "Ekspor semua item, transaksi, dan outlet", Icons.Default.Dataset),
        Triple("Berdasarkan\nPeriode", "Pilih rentang tanggal transaksi", Icons.Default.DateRange),
        Triple("Berdasarkan\nOutlet", "Pilih satu atau beberapa outlet", Icons.Default.Storefront)
    )

    val destinationOptions = listOf(
        Triple("Perangkat ini", "Simpan di storage perangkat", Icons.Default.Smartphone),
        Triple("Google Drive", "Simpan ke Google Drive", null),
        Triple("Huawei Cloud Drive", "Simpan ke Huawei Cloud", Icons.Default.Cloud)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Match background
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Export Excel",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Ekspor data ke file Excel untuk analisis atau pelaporan.",
                        color = TextGray,
                        fontSize = 13.sp
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Column 1: Siapkan File Ekspor
                    Card(
                        modifier = Modifier.weight(1.3f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("1. Siapkan File Ekspor", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text("Nama File", fontSize = 12.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = fileName,
                                    onValueChange = { fileName = it },
                                    leadingIcon = { Icon(Icons.Default.TableView, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0)),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { fileName = "Laporan_Stok.xlsx" },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextGray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", color = TextDark, fontSize = 13.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Ruang Lingkup Ekspor", fontSize = 12.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                scopeOptions.forEach { option ->
                                    val isSel = selectedScope == option.first
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFFF0FDF4) else Color.White)
                                            .border(1.dp, if (isSel) SecondaryTeal else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .clickable { selectedScope = option.first }
                                            .padding(horizontal = 6.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (isSel) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isSel) SecondaryTeal else TextGray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = option.first,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = if (isSel) TextDark else TextGray,
                                                    lineHeight = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(option.second, fontSize = 9.sp, color = TextGray, lineHeight = 11.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            var allSheetsSelected by remember { mutableStateOf(true) }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Pilih Sheet yang akan disertakan", fontSize = 12.sp, color = TextGray)
                                Text(
                                    text = if (allSheetsSelected) "Batal Semua" else "Pilih Semua",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { allSheetsSelected = !allSheetsSelected }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val sheets = listOf(
                                Triple(Icons.Default.Home, "Beranda Ringkas", "Ringkasan dashboard utama"),
                                Triple(Icons.Default.Inventory, "Master Stok", "Data master item dan stok"),
                                Triple(Icons.Default.PostAdd, "Input Transaksi", "Semua data transaksi masuk/keluar"),
                                Triple(Icons.Default.Storefront, "Daftar Outlet", "Informasi semua outlet"),
                                Triple(Icons.Default.ViewList, "Sheet per Outlet", "Sheet detail untuk setiap outlet")
                            )
                            sheets.forEach { sheet ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Icon(
                                        if (allSheetsSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (allSheetsSelected) SecondaryTeal else TextGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(sheet.first, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(sheet.second, fontSize = 13.sp, color = TextDark, modifier = Modifier.width(120.dp))
                                    Text(sheet.third, fontSize = 11.sp, color = TextGray)
                                }
                            }
                        }
                    }

                    // Column 2
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(32.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Output, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("2. Pilih Tujuan Ekspor", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Text("Simpan ke", fontSize = 12.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                destinationOptions.forEach { dest ->
                                    val isSel = selectedDestination == dest.first
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedDestination = dest.first }) {
                                        Icon(
                                            if (isSel) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSel) SecondaryTeal else TextGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(dest.first, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSel) PrimaryBlue else TextDark)
                                            }
                                            Text(dest.second, fontSize = 11.sp, color = TextGray)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Opsi Lain", fontSize = 12.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Email Default (opsional)", fontSize = 11.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextGray) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0))
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action buttons row
                        val coroutineScope = rememberCoroutineScope()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val file = viewModel.getExportedFile(context)
                                        if (file != null) {
                                            val folderUriString = state.backupFolderUri
                                            if (!folderUriString.isNullOrEmpty()) {
                                                try {
                                                    val treeUri = android.net.Uri.parse(folderUriString)
                                                    val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                                                    if (documentFile != null && documentFile.canWrite()) {
                                                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                                        val newFile = documentFile.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Laporan_Stok_LONDRI_$timestamp.xlsx")
                                                        if (newFile != null) {
                                                            val outputStream = context.contentResolver.openOutputStream(newFile.uri)
                                                            val inputStream = java.io.FileInputStream(file)
                                                            inputStream.copyTo(outputStream!!)
                                                            inputStream.close()
                                                            outputStream.close()
                                                            Toast.makeText(context, "Berhasil di-download ke folder: ${state.backupFolderName}", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "Gagal membuat file di folder pilihan.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Izin akses folder tidak aktif. Silakan pilih folder ulang di Backup Drive.", Toast.LENGTH_LONG).show()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Gagal menyimpan ke folder: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Pilih folder backup terlebih dahulu di menu Backup Drive!", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Gagal membuat file Excel", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text("Download", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 12.sp, maxLines = 1)
                                        Text("Simpan ke perangkat", fontSize = 8.sp, lineHeight = 9.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 2)
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val file = viewModel.getExportedFile(context)
                                        if (file != null) {
                                            try {
                                                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val shareIntent = android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Laporan via..."))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Gagal membuat file Excel", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text("Share", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 12.sp, maxLines = 1)
                                        Text("Bagikan file", fontSize = 8.sp, lineHeight = 9.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 2)
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val file = viewModel.getExportedFile(context)
                                        if (file != null) {
                                            try {
                                                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "message/rfc822"
                                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Laporan Stok Outlet Laundry")
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "Halo,\n\nBerikut terlampir file Laporan Stok Outlet Laundry terbaru.\n\nTerima kasih.")
                                                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(emailIntent, "Kirim Email via..."))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Gagal membuka aplikasi email: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Gagal membuat file Excel", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text("Kirim via Email", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 12.sp, maxLines = 1)
                                        Text("Kirim sebagai lampiran", fontSize = 8.sp, lineHeight = 9.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 2)
                                    }
                                }
                            }
                        }
                    }

                    // Column 3: Ringkasan Isi File
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Article, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("3. Ringkasan Isi File", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text("File akan berisi:", fontSize = 12.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val summaryItems = listOf(
                                Triple(Icons.Default.Inventory2, "Total Item", "${state.totalItems}\nItem"),
                                Triple(Icons.Default.Layers, "Total Stok", "0\nPcs / Liter / Unit"),
                                Triple(Icons.Default.ReceiptLong, "Total Transaksi", "${state.totalTransactions}\nTransaksi"),
                                Triple(Icons.Default.Storefront, "Total Outlet", "${state.totalOutlets}\nOutlet"),
                                Triple(Icons.Default.InsertDriveFile, "Total Sheet", "5\nSheet")
                            )
                            
                            summaryItems.forEach { sumItem ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Box(modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(sumItem.first, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(sumItem.second, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextDark, modifier = Modifier.weight(1f))
                                    Column(horizontalAlignment = Alignment.End) {
                                        val parts = sumItem.third.split("\n")
                                        Text(parts[0], fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                                        if (parts.size > 1) {
                                            Text(parts[1], fontSize = 9.sp, color = TextGray)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Data diambil per\nSaat ini", fontSize = 11.sp, color = PrimaryBlue)
                            }
                        }
                    }
                }
            }



            // RIWAYAT EKSPOR
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Riwayat Ekspor Terbaru", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E3A8A)).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tanggal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("Nama File", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                            Text("Ruang Lingkup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                            Text("Tujuan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("Ukuran", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                            Text("Oleh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("Aksi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                        
                        val history = emptyList<Triple<String, String, List<String>>>()
                        if (history.isEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                                Text("Belum ada riwayat", color = TextGray, fontSize = 12.sp)
                            }
                        } else {
                            // Empty for now
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { Toast.makeText(context, "Riwayat ekspor lengkap belum tersedia", Toast.LENGTH_SHORT).show() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lihat semua riwayat", color = PrimaryBlue, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
