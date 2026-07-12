package com.laundry.stockapp.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.data.model.Outlet
import com.laundry.stockapp.data.model.Transaction
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit,
    userName: String = "Pengguna",
    profileImageUri: String? = null,
    viewModel: InputTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedOutlet by remember { mutableStateOf<Outlet?>(null) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    var outletExpanded by remember { mutableStateOf(false) }
    var itemExpanded by remember { mutableStateOf(false) }

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadData()
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadHistoryData()
    }

    // Filter transaction list
    val filteredTransactions = remember(
        state.recentTransactions,
        selectedOutlet,
        selectedItem,
        searchQuery
    ) {
        state.recentTransactions.filter { tx ->
            val matchOutlet = selectedOutlet == null || tx.outletId == selectedOutlet?.id
            val matchItem = selectedItem == null || tx.itemId == selectedItem?.id
            val matchQuery = searchQuery.isEmpty() ||
                    tx.itemName.orEmpty().contains(searchQuery, ignoreCase = true) ||
                    tx.outletName.orEmpty().contains(searchQuery, ignoreCase = true) ||
                    tx.notes.orEmpty().contains(searchQuery, ignoreCase = true)
            matchOutlet && matchItem && matchQuery
        }
    }

    // Handle delete confirmation dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Hapus Transaksi", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Apakah Anda yakin ingin menghapus transaksi pengeluaran item " +
                        "\"${transactionToDelete?.itemName}\" untuk outlet \"${transactionToDelete?.outletName}\"? " +
                        "Stok barang akan otomatis dikembalikan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
        // 1. Top Header
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
                    Text("Riwayat Transaksi", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Daftar seluruh keluar-masuk stok outlet", fontSize = 16.sp, color = TextGray)
                }
            }

            TopProfileBar(
                userName = userName,
                profileImageUri = profileImageUri
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari item, outlet, catatan...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextGray) },
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true
            )

            // Outlet Dropdown Filter
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { outletExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedOutlet?.name ?: "Semua Outlet",
                            fontSize = 13.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextGray)
                    }
                }
                DropdownMenu(
                    expanded = outletExpanded,
                    onDismissRequest = { outletExpanded = false },
                    modifier = Modifier.width(200.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Semua Outlet") },
                        onClick = {
                            selectedOutlet = null
                            outletExpanded = false
                        }
                    )
                    state.outlets.forEach { outlet ->
                        DropdownMenuItem(
                            text = { Text(outlet.name.orEmpty()) },
                            onClick = {
                                selectedOutlet = outlet
                                outletExpanded = false
                            }
                        )
                    }
                }
            }

            // Item Dropdown Filter
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { itemExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedItem?.name ?: "Semua Item",
                            fontSize = 13.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextGray)
                    }
                }
                DropdownMenu(
                    expanded = itemExpanded,
                    onDismissRequest = { itemExpanded = false },
                    modifier = Modifier.width(200.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Semua Item") },
                        onClick = {
                            selectedItem = null
                            itemExpanded = false
                        }
                    )
                    state.items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name.orEmpty()) },
                            onClick = {
                                selectedItem = item
                                itemExpanded = false
                            }
                        )
                    }
                }
            }

            // Reset Button
            if (searchQuery.isNotEmpty() || selectedOutlet != null || selectedItem != null) {
                IconButton(
                    onClick = {
                        searchQuery = ""
                        selectedOutlet = null
                        selectedItem = null
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.FilterAltOff, contentDescription = "Reset Filter", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Table Layout Card
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
                    Text("Nama Outlet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                    Text("Wilayah", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Nama Barang", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                    Text("Qty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                    Text("Catatan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                    Spacer(modifier = Modifier.width(36.dp)) // For delete icon column
                }

                val txDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (filteredTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada transaksi yang ditemukan",
                                    color = TextGray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        itemsIndexed(filteredTransactions) { index, tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(txDateFormat.format(tx.date ?: Date()), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                Text(tx.outletName.orEmpty(), color = TextDark, fontSize = 12.sp, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Medium)
                                Text(tx.region.orEmpty(), color = TextDark, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(tx.itemName.orEmpty(), color = TextDark, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                
                                val txQty = tx.qtyOut ?: 0
                                val isPositive = txQty < 0
                                val displayQty = if (isPositive) "+${-txQty}" else "-${txQty}"
                                Text(
                                    text = displayQty,
                                    color = if (isPositive) SecondaryTeal else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f)
                                )

                                Box(modifier = Modifier.weight(1.2f)) {
                                    val badgeColor = if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFEF2F2)
                                    val badgeTxtColor = if (isPositive) SecondaryTeal else Color(0xFFEF4444)
                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tx.notes.orEmpty().ifEmpty { "-" },
                                            color = badgeTxtColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Delete Action Icon
                                IconButton(
                                    onClick = { transactionToDelete = tx },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Transaksi",
                                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < filteredTransactions.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                            }
                        }
                    }
                }
            }
        }
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
