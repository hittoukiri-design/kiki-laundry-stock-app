// This App was build by Chris Tambayong - Fumakill4
package com.laundry.stockapp.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    profileImageUri: String? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.loadDashboardData()
            kotlinx.coroutines.delay(1000)
            pullToRefreshState.endRefresh()
        }
    } else {
        LaunchedEffect(Unit) {
            viewModel.loadDashboardData()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stok Outlet Laundry",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Text(
                                text = "Kelola stok barang outlet laundry dengan mudah",
                                color = TextGray,
                                fontSize = 13.sp
                            )
                        }
                        TopProfileBar(
                            userName = state.userName,
                            profileImageUri = profileImageUri,
                            onLogout = { viewModel.logout() }
                        )
                    }
                }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.Inventory2,
                        iconColor = PrimaryBlue,
                        iconBg = Color(0xFFEFF6FF),
                        title = "Total Item Aktif",
                        value = state.totalActiveItems.toString(),
                        subtitle = "Item",
                        onDetailClick = { navController.navigate("master_item") }
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.Layers,
                        iconColor = SecondaryTeal,
                        iconBg = Color(0xFFE0F2FE),
                        title = "Total Stok Awal",
                        value = state.totalStartingStock.toString(),
                        subtitle = "Pcs / Liter / Unit",
                        onDetailClick = { navController.navigate("master_item") }
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.Output,
                        iconColor = Color(0xFF6366F1),
                        iconBg = Color(0xFFF3E8FF),
                        title = "Total Barang Keluar",
                        value = state.totalOut.toString(),
                        subtitle = "Pcs / Liter / Unit",
                        onDetailClick = { navController.navigate("input_transaction") }
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.List,
                        iconColor = SecondaryTeal,
                        iconBg = Color(0xFFE0F2FE),
                        title = "Total Sisa Stok",
                        value = state.totalRemainingStock.toString(),
                        subtitle = "Pcs / Liter / Unit",
                        onDetailClick = { navController.navigate("master_item") }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.AddCircleOutline,
                        title = "Tambah Item",
                        subtitle = "Tambah item baru",
                        bgColor = PrimaryBlue,
                        onClick = { navController.navigate("master_item") }
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.PostAdd,
                        title = "Input Transaksi",
                        subtitle = "Catat stok masuk/keluar",
                        bgColor = SecondaryTeal,
                        onClick = { navController.navigate("input_transaction") }
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.Description,
                        title = "Export Excel",
                        subtitle = "Export laporan stok",
                        bgColor = Color(0xFF0D9488),
                        onClick = { navController.navigate("export") }
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.CloudUpload,
                        title = "Backup Sekarang",
                        subtitle = "Simpan data ke drive",
                        bgColor = Color(0xFF8B5CF6),
                        onClick = { navController.navigate("backup") }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(Color(0xFFE0E7FF), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Status Sync", fontSize = 11.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Tersinkron", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SecondaryTeal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Data terbaru telah tersimpan", fontSize = 11.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.weight(1.2f).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).background(Color(0xFFEFF6FF), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Backup Terakhir", fontSize = 11.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(state.lastBackupAt ?: "Belum ada backup", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Oleh: ${if (state.lastBackupAt != null) state.userName else "-"}", fontSize = 11.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFFE2E8F0))
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sync Tertunda", fontSize = 11.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("0 Transaksi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Akan disinkronkan otomatis", fontSize = 11.sp, color = TextGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1.8f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Transaksi Terbaru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { navController.navigate("input_transaction") }) {
                                    Text("Lihat semua", color = PrimaryBlue, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().background(PrimaryBlue).padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tanggal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                                Text("Outlet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                Text("Item", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.8f))
                                Text("Qty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                                Text("Catatan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                            }
                            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            val transactions = state.recentTransactions.take(5)
                            if (transactions.isEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                                    Text("Belum ada transaksi", color = TextGray, fontSize = 12.sp)
                                }
                            } else {
                                transactions.forEachIndexed { index, tx ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(dateFormat.format(tx.date ?: java.util.Date()), color = TextDark, fontSize = 11.sp, modifier = Modifier.weight(1.2f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(tx.outletName.orEmpty(), color = TextDark, fontSize = 11.sp, modifier = Modifier.weight(1.5f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(tx.itemName.orEmpty(), color = TextDark, fontSize = 11.sp, modifier = Modifier.weight(1.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        val qty = tx.qtyOut ?: 0
                                        val isPositive = qty < 0
                                        val displayQty = if (isPositive) "+${-qty}" else "-${qty}"
                                        Text(displayQty, color = if (isPositive) SecondaryTeal else Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                                        Text(tx.notes.orEmpty(), color = TextGray, fontSize = 11.sp, modifier = Modifier.weight(1.5f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (index < transactions.size - 1) {
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Status Stok", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Aman
                            StatusBox(
                                color = SecondaryTeal,
                                icon = Icons.Default.CheckCircle,
                                title = "Aman",
                                subtitle = "Stok dalam kondisi aman",
                                value = state.statusSafeCount.toString()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Perhatian
                            StatusBox(
                                color = Color(0xFFF59E0B),
                                icon = Icons.Default.Warning,
                                title = "Perhatian",
                                subtitle = "Stok menipis, perlu restock",
                                value = state.statusWarningCount.toString()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Habis/Minus
                            StatusBox(
                                color = Color(0xFFEF4444),
                                icon = Icons.Default.Cancel,
                                title = "Habis/Minus",
                                subtitle = "Stok habis atau minus",
                                value = state.statusDangerCount.toString()
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clickable { navController.navigate("master_item") },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lihat semua item", color = SecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        }
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    value: String,
    subtitle: String,
    onDetailClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(36.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, maxLines = 1)
                    Text(subtitle, fontSize = 10.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onDetailClick() }) {
                Text("Lihat detail", color = PrimaryBlue, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 9.sp, color = Color.White.copy(alpha=0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun StatusBox(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().border(1.dp, color.copy(alpha=0.3f), RoundedCornerShape(8.dp)).background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 10.sp, color = TextGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text("Item", fontSize = 10.sp, color = color)
        }
    }
}
