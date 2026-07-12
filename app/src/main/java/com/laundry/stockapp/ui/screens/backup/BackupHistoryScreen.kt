package com.laundry.stockapp.ui.screens.backup

// This App was build by Chris Tambayong - Fumakill4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.screens.settings.SettingsViewModel
import com.laundry.stockapp.ui.theme.PrimaryBlue
import com.laundry.stockapp.ui.theme.TextGray
import com.laundry.stockapp.ui.theme.TextDark
import com.laundry.stockapp.ui.theme.SecondaryTeal

@Composable
fun BackupHistoryScreen(
    onBack: () -> Unit,
    userName: String,
    profileImageUri: String?,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val history = state.backupHistory
    var selectedLogForDetail by remember { mutableStateOf<com.laundry.stockapp.data.model.BackupLog?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = PrimaryBlue,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Riwayat Backup", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Daftar lengkap aktivitas sinkronisasi dan backup", fontSize = 16.sp, color = TextGray)
                }
            }

            TopProfileBar(
                userName = userName,
                profileImageUri = profileImageUri
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Content Table Card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tanggal & Waktu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                    Text("Jenis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Ukuran", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Oleh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(32.dp))
                }

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat backup tersedia",
                            fontSize = 16.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(history) { index, log ->
                            val user = log.operator ?: "Sistem"
                            val isSuccess = log.status == "Berhasil"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLogForDetail = log }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.timeFormatted.orEmpty(), color = TextDark, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                Text(log.type.orEmpty(), color = TextDark, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(log.size.orEmpty(), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isSuccess) SecondaryTeal else Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = log.status.orEmpty(),
                                        color = if (isSuccess) SecondaryTeal else Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(user, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { selectedLogForDetail = log },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Detail", tint = TextGray, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < history.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
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
