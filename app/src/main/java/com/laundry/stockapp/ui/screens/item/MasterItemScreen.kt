package com.laundry.stockapp.ui.screens.item

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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterItemScreen(
    userName: String = "",
    profileImageUri: String? = null,
    viewModel: MasterItemViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }
    var filterStatus by remember { mutableStateOf("Semua") }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadItems()
            kotlinx.coroutines.delay(1000)
            pullToRefreshState.endRefresh()
        }
    }

    // Local dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }
    var deletePasswordInput by remember { mutableStateOf("") }

    // Sync delete-password dialog with ViewModel
    LaunchedEffect(state.showPasswordDialog) {
        if (state.showPasswordDialog) deletePasswordInput = ""
    }

    // Show errors as Toast
    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // ── Add / Edit Item Dialog ────────────────────────────────────────────────
    if (showAddEditDialog) {
        var nameInput by remember(itemToEdit) { mutableStateOf(itemToEdit?.name ?: "") }
        var stockInput by remember(itemToEdit) {
            mutableStateOf(itemToEdit?.startingStock?.toString() ?: "")
        }

        // Cek duplikat: nama sama (case-insensitive) tapi bukan item yang sedang diedit
        val isDuplicate = nameInput.isNotBlank() && state.items.any { existingItem ->
            existingItem.name.orEmpty().trim().equals(nameInput.trim(), ignoreCase = true) &&
            existingItem.id != (itemToEdit?.id ?: "")
        }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false; itemToEdit = null },
            title = {
                Text(
                    text = if (itemToEdit == null) "Tambah Item" else "Edit Item",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryBlue
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Item") },
                        singleLine = true,
                        isError = isDuplicate,
                        supportingText = if (isDuplicate) {
                            { Text("Item \"${nameInput.trim()}\" sudah terdaftar", color = Color(0xFFEF4444), fontSize = 11.sp) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDuplicate) Color(0xFFEF4444) else PrimaryBlue,
                            unfocusedBorderColor = if (isDuplicate) Color(0xFFEF4444) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Stok Awal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDuplicate,
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val stock = stockInput.toIntOrNull() ?: 0
                            val newItem = itemToEdit
                                ?.copy(name = nameInput.trim(), startingStock = stock)
                                ?: Item(name = nameInput.trim(), startingStock = stock)
                            viewModel.saveItem(newItem)
                            showAddEditDialog = false
                            itemToEdit = null
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Nama item tidak boleh kosong",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("Simpan") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddEditDialog = false; itemToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // ── Delete Password Dialog (driven by ViewModel state) ───────────────────
    if (state.showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = {
                Text(
                    "Konfirmasi Hapus",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Masukkan master password untuk menghapus item \"${state.itemToDelete?.name ?: ""}\".",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = deletePasswordInput,
                        onValueChange = { deletePasswordInput = it },
                        label = { Text("Master Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete(deletePasswordInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelDelete() }) { Text("Batal") }
            }
        )
    }

    // ── Main Screen Layout ────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
            .background(Color(0xFFF8FAFC))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────────
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
                        text = "Master Item",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Kelola data master item stok outlet laundry",
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

        // ── Scrollable Body ───────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {

            // ── ROW 1: Four Summary Cards ─────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1 — Total Item
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFF1F5F9)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Total Item",
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    state.totalItem.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Text("Item", fontSize = 10.sp, color = TextGray)
                            }
                        }
                    }

                    // Card 2 — Total Stok Awal
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFF1F5F9)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE0F2FE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Total Stok Awal",
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    state.totalStartingStock.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Text(
                                    "Pcs / Liter",
                                    fontSize = 10.sp,
                                    color = TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Card 3 — Total Keluar
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFF1F5F9)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF3E8FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Output,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Total Keluar",
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    state.totalOut.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Text(
                                    "Pcs / Liter",
                                    fontSize = 10.sp,
                                    color = TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Card 4 — Status Stok
                    Card(
                        modifier = Modifier.weight(1.3f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFF1F5F9)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Status Stok",
                                fontSize = 12.sp,
                                color = TextDark,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SecondaryTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        state.statusSafeCount.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SecondaryTeal
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text("Aman", fontSize = 10.sp, color = TextGray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        state.statusWarningCount.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFD97706)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text("Perhatian", fontSize = 10.sp, color = TextGray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        state.statusDangerCount.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text("Minus", fontSize = 10.sp, color = TextGray)
                                }
                            }
                        }
                    }
                }
            }

            // ── ROW 2: Search + Filter + Sort + Tambah ───────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
                        },
                        placeholder = {
                            Text("Cari...", color = TextGray, fontSize = 13.sp, maxLines = 1)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.width(12.dp))

                    Row(
                        modifier = Modifier.weight(2f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Filter button with dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { filterMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, if (filterStatus != "Semua") PrimaryBlue else Color(0xFFE2E8F0)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = if (filterStatus != "Semua") PrimaryBlue else TextDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (filterStatus == "Semua") "Filter" else filterStatus,
                                    color = if (filterStatus != "Semua") PrimaryBlue else TextDark,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = filterMenuExpanded,
                                onDismissRequest = { filterMenuExpanded = false }
                            ) {
                                listOf("Semua", "Aman", "Perhatian", "Habis/Minus").forEach { status ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (filterStatus == status) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                }
                                                Text(
                                                    status,
                                                    fontSize = 12.sp,
                                                    color = if (filterStatus == status) PrimaryBlue else TextDark,
                                                    fontWeight = if (filterStatus == status) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        onClick = {
                                            filterStatus = status
                                            filterMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { sortAscending = !sortAscending },
                            modifier = Modifier.weight(1.5f).fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFE2E8F0)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                if (sortAscending) Icons.Default.SwapVert else Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = TextDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (sortAscending) "Urutkan A-Z" else "Urutkan Z-A",
                                color = TextDark,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (sortAscending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = TextDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            itemToEdit = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Tambah",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── ROW 3: Data Table ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Blue Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimaryBlue)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(2f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Nama Item",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.UnfoldMore,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                "Stok Awal",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                "Total Keluar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                "Sisa Stok",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                "Status",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1.5f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Aksi",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }

                        val displayItems = state.items
                            .filter { it.name.orEmpty().contains(searchQuery, ignoreCase = true) }
                            .filter { item ->
                                when (filterStatus) {
                                    "Aman"        -> item.remainingStock >= 5
                                    "Perhatian"   -> item.remainingStock in 1..4
                                    "Habis/Minus" -> item.remainingStock <= 0
                                    else          -> true
                                }
                            }
                            .let { list ->
                                if (sortAscending) list.sortedBy { it.name.orEmpty().lowercase() }
                                else list.sortedByDescending { it.name.orEmpty().lowercase() }
                            }

                        if (displayItems.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Tidak ada item", color = TextGray, fontSize = 12.sp)
                            }
                        } else {
                            displayItems.forEachIndexed { index, item ->
                                val rem = item.remainingStock
                                val statusStr = when {
                                    rem >= 5  -> "Aman"
                                    rem in 1..4 -> "Perhatian"
                                    else      -> "Habis/Minus"
                                }
                                val statusColor = when (statusStr) {
                                    "Aman"      -> SecondaryTeal
                                    "Perhatian" -> Color(0xFFF59E0B)
                                    else        -> Color(0xFFEF4444)
                                }
                                val statusIcon = when (statusStr) {
                                    "Aman"      -> Icons.Default.CheckCircle
                                    "Perhatian" -> Icons.Default.Warning
                                    else        -> Icons.Default.Cancel
                                }
                                val statusBg = when (statusStr) {
                                    "Aman"      -> Color(0xFFF0FDF4)
                                    "Perhatian" -> Color(0xFFFFFBEB)
                                    else        -> Color(0xFFFEF2F2)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.name.orEmpty(),
                                        color = TextDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(2f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        item.startingStock.toString(),
                                        color = TextDark,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        item.totalOut.toString(),
                                        color = TextDark,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        rem.toString(),
                                        color = if (rem < 0) Color(0xFFEF4444) else TextDark,
                                        fontWeight = if (rem < 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Status badge
                                    Row(
                                        modifier = Modifier.weight(1.5f),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .background(statusBg, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                statusIcon,
                                                contentDescription = null,
                                                tint = statusColor,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                statusStr,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Action icons
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    itemToEdit = item
                                                    showAddEditDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit ${item.name}",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    viewModel.requestDelete(item)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Hapus ${item.name}",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                if (index < displayItems.size - 1) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                }
                            }
                        }
                    }
                }
            }

            // ── ROW 4: Warning Box ────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Perhatian: Menghapus item akan menghapus data terkait (riwayat transaksi). Tindakan ini memerlukan master password.",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        }
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
