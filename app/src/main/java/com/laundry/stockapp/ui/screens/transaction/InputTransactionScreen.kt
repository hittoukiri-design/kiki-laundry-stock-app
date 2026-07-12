package com.laundry.stockapp.ui.screens.transaction

import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.data.model.Outlet
import com.laundry.stockapp.ui.components.TopProfileBar
import com.laundry.stockapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputTransactionScreen(
    userName: String = "",
    profileImageUri: String? = null,
    onNavigateToHistory: () -> Unit = {},
    viewModel: InputTransactionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Form state
    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var dateText by remember { mutableStateOf(dateFormat.format(calendar.time)) }

    var selectedOutlet by remember { mutableStateOf<Outlet?>(null) }
    var outletExpanded by remember { mutableStateOf(false) }

    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var itemExpanded by remember { mutableStateOf(false) }

    var qty by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Date Picker Dialog
    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.time
                dateText = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Handle success/error
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            android.widget.Toast.makeText(context, "Transaksi berhasil disimpan!", android.widget.Toast.LENGTH_SHORT).show()
            selectedOutlet = null
            selectedItem = null
            qty = ""
            notes = ""
            viewModel.clearSuccess()
        }
    }
    LaunchedEffect(state.error) {
        if (state.error != null) {
            android.widget.Toast.makeText(context, "Error: ${state.error}", android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Input Transaksi",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Catat stok keluar dengan mudah dan cepat",
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
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ── LEFT COLUMN (FORM) ──
                    Column(modifier = Modifier.weight(1.1f)) {

                        // Tanggal (clickable → DatePickerDialog)
                        FormLabel(title = "Tanggal", icon = Icons.Default.CalendarToday, iconBg = Color(0xFFEFF6FF), iconTint = PrimaryBlue, isRequired = false)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 44.dp)
                                .height(44.dp)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .clickable { datePicker.show() }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(dateText, fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Outlet (Dropdown dari Firestore)
                        FormLabel(title = "Outlet", icon = Icons.Default.Storefront, iconBg = Color(0xFFEFF6FF), iconTint = PrimaryBlue, isRequired = true)
                        Box(modifier = Modifier.fillMaxWidth().padding(start = 44.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = outletExpanded,
                                onExpandedChange = { outletExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedOutlet?.name ?: "Pilih outlet",
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        color = if (selectedOutlet != null) TextDark else TextGray
                                    ),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = outletExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = outletExpanded,
                                    onDismissRequest = { outletExpanded = false }
                                ) {
                                    if (state.outlets.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Memuat outlet...", fontSize = 12.sp, color = TextGray) },
                                            onClick = {}
                                        )
                                    } else {
                                        state.outlets.sortedBy { it.name.orEmpty() }.forEach { o ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(o.name.orEmpty(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                                        Text(o.region.orEmpty(), fontSize = 10.sp, color = TextGray)
                                                    }
                                                },
                                                onClick = {
                                                    selectedOutlet = o
                                                    outletExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text("Pilih outlet tujuan pengeluaran stok", fontSize = 9.sp, color = TextGray, modifier = Modifier.padding(start = 44.dp, top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Wilayah (auto-fill dari outlet)
                        FormLabel(title = "Wilayah", icon = Icons.Default.LocationOn, iconBg = Color(0xFFDCFCE7), iconTint = SecondaryTeal, isRequired = false)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 44.dp)
                                .height(44.dp)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedOutlet?.region ?: "",
                                    fontSize = 12.sp,
                                    color = if (selectedOutlet != null) TextDark else TextGray,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedOutlet != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Otomatis", color = SecondaryTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(modifier = Modifier.size(12.dp).background(SecondaryTeal, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Text("Wilayah terdeteksi otomatis berdasarkan outlet", fontSize = 9.sp, color = SecondaryTeal, modifier = Modifier.padding(start = 44.dp, top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Item (Dropdown dari Firestore)
                        FormLabel(title = "Item", icon = Icons.Default.Inventory2, iconBg = Color(0xFFF3E8FF), iconTint = Color(0xFF6366F1), isRequired = true)
                        Box(modifier = Modifier.fillMaxWidth().padding(start = 44.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = itemExpanded,
                                onExpandedChange = { itemExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedItem?.name ?: "Pilih item",
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        color = if (selectedItem != null) TextDark else TextGray
                                    ),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = itemExpanded,
                                    onDismissRequest = { itemExpanded = false }
                                ) {
                                    if (state.items.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Memuat item...", fontSize = 12.sp, color = TextGray) },
                                            onClick = {}
                                        )
                                    } else {
                                        state.items.sortedBy { it.name.orEmpty() }.forEach { i ->
                                            DropdownMenuItem(
                                                text = { Text(i.name.orEmpty(), fontSize = 12.sp, color = TextDark) },
                                                onClick = {
                                                    selectedItem = i
                                                    itemExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text("Pilih item yang dikeluarkan", fontSize = 9.sp, color = TextGray, modifier = Modifier.padding(start = 44.dp, top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Qty Keluar
                        FormLabel(title = "Qty Keluar", icon = Icons.Default.Layers, iconBg = Color(0xFFDCFCE7), iconTint = SecondaryTeal, isRequired = true)
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { if (it.all { c -> c.isDigit() }) qty = it },
                            modifier = Modifier.fillMaxWidth().height(48.dp).padding(start = 44.dp),
                            trailingIcon = { Text("Pcs", color = TextDark, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0)),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        Text("Masukkan jumlah stok yang keluar", fontSize = 9.sp, color = SecondaryTeal, modifier = Modifier.padding(start = 44.dp, top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Catatan
                        FormLabel(title = "Catatan", icon = Icons.Default.Description, iconBg = Color(0xFFF3E8FF), iconTint = Color(0xFF6366F1), isRequired = false)
                        Box(modifier = Modifier.padding(start = 44.dp)) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { if (it.length <= 150) notes = it },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0)),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                maxLines = 2
                            )
                            Text(
                                "${notes.length}/150",
                                fontSize = 8.sp,
                                color = TextGray,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 6.dp)
                            )
                        }
                        Text("Catatan opsional", fontSize = 9.sp, color = TextGray, modifier = Modifier.padding(start = 44.dp, top = 2.dp))
                        Spacer(modifier = Modifier.height(6.dp))

                        Text("* Wajib diisi", fontSize = 9.sp, color = TextGray, modifier = Modifier.padding(start = 44.dp))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Simpan Button
                        Button(
                            onClick = {
                                val o = selectedOutlet
                                val i = selectedItem
                                val q = qty.toIntOrNull() ?: 0
                                when {
                                    o == null -> android.widget.Toast.makeText(context, "Pilih outlet terlebih dahulu", android.widget.Toast.LENGTH_SHORT).show()
                                    i == null -> android.widget.Toast.makeText(context, "Pilih item terlebih dahulu", android.widget.Toast.LENGTH_SHORT).show()
                                    q <= 0 -> android.widget.Toast.makeText(context, "Qty harus lebih dari 0", android.widget.Toast.LENGTH_SHORT).show()
                                    else -> viewModel.saveTransaction(selectedDate, o, i, q, notes)
                                }
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth().height(46.dp).padding(start = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan Transaksi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(start = 44.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync & backup otomatis berjalan saat data disimpan", color = SecondaryTeal, fontSize = 9.sp)
                        }
                    }

                    // ── RIGHT COLUMN ──
                    Column(modifier = Modifier.weight(1f)) {
                        // Card 1: Update Stok Otomatis
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Update, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Update Stok Otomatis", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SecondaryTeal)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    val stokSebelum = selectedItem?.remainingStock?.toString() ?: "-"
                                    val outQty = qty.toIntOrNull() ?: 0
                                    val stokSetelah = if (selectedItem != null) (selectedItem!!.remainingStock - outQty).toString() else "-"
                                    
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Stok Sebelum", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(stokSebelum, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                        Text("Pcs", fontSize = 10.sp, color = PrimaryBlue)
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Stok Setelah", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(stokSetelah, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                        Text("Pcs", fontSize = 10.sp, color = SecondaryTeal)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(14.dp).background(SecondaryTeal, CircleShape), contentAlignment = Alignment.Center) {
                                        Text("i", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stok akan berkurang otomatis setelah transaksi disimpan.", fontSize = 9.sp, color = SecondaryTeal)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Card 2: Transaksi Terbaru
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Transaksi Terbaru", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            onNavigateToHistory()
                                        }
                                    ) {
                                        Text("Lihat semua", color = PrimaryBlue, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                val txDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                val transactions = state.recentTransactions.take(5)

                                if (transactions.isEmpty()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                                        Text("Belum ada transaksi", color = TextGray, fontSize = 12.sp)
                                    }
                                } else {
                                    transactions.forEachIndexed { index, tx ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(24.dp).background(Color(0xFFF3E8FF), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(txDateFormat.format(tx.date ?: Date()), color = TextGray, fontSize = 9.sp, modifier = Modifier.weight(1.2f))
                                            Text(tx.outletName.orEmpty(), color = TextDark, fontSize = 9.sp, modifier = Modifier.weight(1.5f))
                                            Text(tx.itemName.orEmpty(), color = TextDark, fontSize = 9.sp, modifier = Modifier.weight(1.5f))
                                            val txQty = tx.qtyOut ?: 0
                                            val isPositive = txQty < 0
                                            val displayQty = if (isPositive) "+${-txQty}" else "-${txQty}"
                                            Text(displayQty, color = if (isPositive) SecondaryTeal else Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                                val badgeColor = if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFEF2F2)
                                                val badgeTxtColor = if (isPositive) SecondaryTeal else Color(0xFFEF4444)
                                                Box(modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                    Text(tx.notes.orEmpty().ifEmpty { "-" }, color = badgeTxtColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        if (index < transactions.size - 1) {
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onNavigateToHistory()
                                    },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Lihat semua transaksi", color = PrimaryBlue, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Card 3: Data Aman & Tersinkron
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).background(Color(0xFF0EA5E9), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Data Aman & Tersinkron", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0369A1))
                                    Text("Setiap transaksi yang disimpan akan otomatis tersinkron ke semua perangkat dan backup ke Google Drive.", fontSize = 9.sp, color = Color(0xFF0369A1), lineHeight = 13.sp)
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

@Composable
fun FormLabel(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    isRequired: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Box(modifier = Modifier.size(32.dp).background(iconBg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                append(title)
                if (isRequired) withStyle(SpanStyle(color = Color(0xFFEF4444))) { append(" *") }
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )
    }
}
