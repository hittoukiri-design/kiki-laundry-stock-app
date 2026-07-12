// This App was build by Chris Tambayong - Fumakill4
package com.laundry.stockapp.ui.screens.outlet

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laundry.stockapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletDetailScreen(
    outletId: String,
    initialTab: Int = 0,
    viewModel: OutletViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.detailState.collectAsState()
    val context = LocalContext.current
    var selectedTabState by remember { mutableStateOf(initialTab) }
    val coroutineScope = rememberCoroutineScope()

    val sdfDateOnly = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }

    // Dialog states for weekly checklist
    var showAddWeeklyDialog by remember { mutableStateOf(false) }
    var showEditWeeklyDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<com.laundry.stockapp.data.model.MaintenanceItem?>(null) }
    var weeklyItemName by remember { mutableStateOf("") }

    var showDeleteWeeklyDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<com.laundry.stockapp.data.model.MaintenanceItem?>(null) }

    // Dialog states for regulator check
    var showRegulatorDialog by remember { mutableStateOf(false) }
    var regulatorDay by remember { mutableStateOf("") }
    var regulatorMonth by remember { mutableStateOf("") }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    // Dialog states for APAR check
    var showAparDialog by remember { mutableStateOf(false) }
    var aparDay by remember { mutableStateOf("") }
    var aparMonth by remember { mutableStateOf("") }
    var aparYear by remember { mutableStateOf("") }
    var aparIntervalMonths by remember { mutableStateOf("") }

    LaunchedEffect(outletId) {
        viewModel.loadOutletDetail(outletId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kembali", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Detail Outlet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (state.outlet != null) {
            // Outlet Basic Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.outlet!!.name.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Wilayah: ${state.outlet!!.region.orEmpty()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextGray
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "ID: ${state.outlet!!.id.take(8).uppercase()}",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Pengambilan", fontSize = 11.sp, color = TextGray)
                            Text(
                                text = "${state.totalQtyOut} item",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = WarningYellow
                            )
                        }
                        Column {
                            Text("Jumlah Transaksi", fontSize = 11.sp, color = TextGray)
                            Text(
                                text = "${state.transactions.size} Transaksi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabState,
                containerColor = Color.White,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                        color = PrimaryBlue
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabState == 0,
                    onClick = { selectedTabState = 0 },
                    text = {
                        Text(
                            text = "Riwayat Transaksi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    selectedContentColor = PrimaryBlue,
                    unselectedContentColor = TextGray
                )
                Tab(
                    selected = selectedTabState == 1,
                    onClick = { selectedTabState = 1 },
                    text = {
                        Text(
                            text = "Ceklist & Pemeliharaan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    selectedContentColor = PrimaryBlue,
                    unselectedContentColor = TextGray
                )
            }

            // Tab Contents
            if (selectedTabState == 0) {
                // Tab 1: Riwayat Transaksi
                val groupedTransactions = state.filteredTransactions.groupBy { sdfDateOnly.format(it.date ?: Date()) }
                var expandedDates by remember { mutableStateOf(setOf<String>()) }

                if (groupedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat transaksi tersedia",
                            color = TextGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        groupedTransactions.forEach { (dateStr, transactions) ->
                            val isExpanded = expandedDates.contains(dateStr)
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedDates = if (isExpanded) {
                                                expandedDates - dateStr
                                            } else {
                                                expandedDates + dateStr
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) PrimaryBlue.copy(alpha = 0.04f) else Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isExpanded) PrimaryBlue.copy(alpha = 0.2f) else Color(0xFFE2E8F0)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = dateStr,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextDark
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            Text(
                                                text = "${transactions.size} Item",
                                                color = TextGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isExpanded) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                                        color = Color.White,
                                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFF8FAFC))
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Waktu", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                                Text("Barang", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                                Text("Qty", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                                Text("Catatan", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                            }
                                            Divider(color = Color(0xFFE2E8F0))
                                            
                                            transactions.forEach { trans ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(trans.date ?: Date()),
                                                        modifier = Modifier.weight(1f),
                                                        fontSize = 13.sp,
                                                        color = TextDark
                                                    )
                                                    Text(
                                                        text = trans.itemName.orEmpty(),
                                                        modifier = Modifier.weight(2f),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextDark
                                                    )
                                                    Text(
                                                        text = (trans.qtyOut ?: 0).toString(),
                                                        modifier = Modifier.weight(0.7f),
                                                        color = WarningYellow,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = trans.notes.orEmpty().ifEmpty { "-" },
                                                        modifier = Modifier.weight(2f),
                                                        fontSize = 13.sp,
                                                        color = TextGray
                                                    )
                                                }
                                                Divider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 2: Ceklist & Pemeliharaan
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Weekly Checklist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ceklist Pemeliharaan Mingguan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Daftar item rutin yang perlu dibersihkan/cek",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                            TextButton(
                                onClick = {
                                    weeklyItemName = ""
                                    showAddWeeklyDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.maintenanceItems.isEmpty()) {
                            Text(
                                text = "Belum ada item pemeliharaan",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            state.maintenanceItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val isCheckedRecently = item.lastMaintenanceAt != null &&
                                                (Date().time - item.lastMaintenanceAt.time < 7 * 24 * 60 * 60 * 1000)

                                        IconButton(
                                            onClick = {
                                                viewModel.checkMaintenanceItem(outletId, item.id)
                                                val msg = if (isCheckedRecently) "Ceklist ${item.name} dibatalkan." else "${item.name} berhasil dicek."
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCheckedRecently) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Tandai Selesai",
                                                tint = if (isCheckedRecently) SafeGreen else TextGray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = item.name.orEmpty(),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = TextDark
                                            )
                                            val lastCheckStr = item.lastMaintenanceAt?.let { "Terakhir dicek: ${sdfDateOnly.format(it)}" } ?: "Belum pernah dicek"
                                            Text(lastCheckStr, fontSize = 11.sp, color = TextGray)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                itemToEdit = item
                                                weeklyItemName = item.name.orEmpty()
                                                showEditWeeklyDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Ubah", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                itemToDelete = item
                                                showDeleteWeeklyDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = DangerRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Regulator Gas Soap Test
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Uji Kebocoran Regulator Gas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Cek secara berkala menggunakan sabun Sunlight",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }

                            TextButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    regulatorDay = state.regulatorCheck?.lastTestDay?.toString() ?: cal.get(Calendar.DAY_OF_MONTH).toString()
                                    regulatorMonth = state.regulatorCheck?.lastTestMonth?.toString() ?: (cal.get(Calendar.MONTH) + 1).toString()
                                    showRegulatorDialog = true
                                }
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Catat Tes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val reg = state.regulatorCheck
                        if (reg != null && reg.lastTestDay != null && reg.lastTestMonth != null && reg.lastTestYear != null) {
                            val cal = Calendar.getInstance()
                            cal.set(reg.lastTestYear, reg.lastTestMonth - 1, reg.lastTestDay)
                            val formattedDate = sdfDateOnly.format(cal.time)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Terakhir Pengujian Sukses:", fontSize = 11.sp, color = TextGray)
                                    Text(formattedDate, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Status Uji Gas:", fontSize = 11.sp, color = TextGray)
                                    Text("Belum pernah diuji / dicatat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                                }
                            }
                        }
                    }

                    // Section 3: APAR Refill Tracker
                    val apar = state.aparCheck
                    var diffDays = 0
                    var isAparConfigured = false
                    var nextRefillDate: Date? = null
                    var isExpired = false
                    var isNearExpiry = false

                    if (apar != null && apar.lastRefillDate != null) {
                        isAparConfigured = true
                        val cal = Calendar.getInstance()
                        cal.time = apar.lastRefillDate
                        cal.add(Calendar.MONTH, apar.intervalMonths ?: 36)
                        nextRefillDate = cal.time

                        val diffTime = nextRefillDate.time - Date().time
                        diffDays = (diffTime.toDouble() / (1000 * 60 * 60 * 24)).toInt()

                        if (diffDays < 0) {
                            isExpired = true
                        } else if (diffDays <= 30) {
                            isNearExpiry = true
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Pemeliharaan Tabung APAR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Masa berlaku pengisian ulang tabung pemadam",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }

                            TextButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    if (apar != null && apar.lastRefillDate != null) {
                                        val rCal = Calendar.getInstance()
                                        rCal.time = apar.lastRefillDate
                                        aparDay = rCal.get(Calendar.DAY_OF_MONTH).toString()
                                        aparMonth = (rCal.get(Calendar.MONTH) + 1).toString()
                                        aparYear = rCal.get(Calendar.YEAR).toString()
                                        aparIntervalMonths = (apar.intervalMonths ?: 36).toString()
                                    } else {
                                        aparDay = cal.get(Calendar.DAY_OF_MONTH).toString()
                                        aparMonth = (cal.get(Calendar.MONTH) + 1).toString()
                                        aparYear = cal.get(Calendar.YEAR).toString()
                                        aparIntervalMonths = "36"
                                    }
                                    showAparDialog = true
                                }
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Data", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAparConfigured && nextRefillDate != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.1f)) {
                                    Text("Terakhir Pengisian:", fontSize = 11.sp, color = TextGray)
                                    Text(
                                        text = sdfDateOnly.format(apar!!.lastRefillDate!!),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                }
                                Column(modifier = Modifier.weight(0.9f)) {
                                    Text("Masa Berlaku:", fontSize = 11.sp, color = TextGray)
                                    val interval = apar?.intervalMonths ?: 36
                                    val yearVal = interval.toDouble() / 12
                                    val yearStr = if (interval % 12 == 0) "${interval / 12}" else String.format(Locale.US, "%.1f", yearVal)
                                    Text(
                                        text = "$interval Bln ($yearStr Thn)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Isi Ulang Berikutnya:", fontSize = 11.sp, color = TextGray)
                                    Text(
                                        text = sdfDateOnly.format(nextRefillDate),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isExpired) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = DangerRed, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Peringatan: APAR kedaluwarsa. Segera lakukan pengisian ulang.",
                                            color = DangerRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else if (isNearExpiry) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Peringatan: APAR kedaluwarsa dalam $diffDays hari.",
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Masa berlaku APAR aman (Sisa $diffDays hari)",
                                            color = Color(0xFF15803D),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Status APAR:", fontSize = 11.sp, color = TextGray)
                                    Text("Data APAR belum dikonfigurasi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                                }
                            }
                        }
                    }

                    // Bottom Autosave Status and Selesai Button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Penyimpanan otomatis aktif. Semua perubahan tersimpan secara real-time ke sistem lokal dan cloud.",
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            var isSavingData by remember { mutableStateOf(false) }
                            
                            Button(
                                onClick = {
                                    isSavingData = true
                                    coroutineScope.launch {
                                        // Delay briefly to allow Firestore cache write to settle and show visual commitment
                                        kotlinx.coroutines.delay(300)
                                        isSavingData = false
                                        Toast.makeText(context, "Semua data pemeliharaan berhasil disimpan secara aman.", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSavingData
                            ) {
                                if (isSavingData) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Menyimpan...", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simpan & Selesai", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Dialog 1: Add Weekly Item
    if (showAddWeeklyDialog) {
        AlertDialog(
            onDismissRequest = { showAddWeeklyDialog = false },
            title = { Text("Tambah Item Pemeliharaan", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column {
                    Text("Masukkan nama alat atau area yang perlu dirawat rutin:", fontSize = 13.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = weeklyItemName,
                        onValueChange = { weeklyItemName = it },
                        placeholder = { Text("Contoh: Mesin Cuci C1") },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (weeklyItemName.isNotBlank()) {
                            viewModel.saveMaintenanceItem(outletId, weeklyItemName.trim())
                            showAddWeeklyDialog = false
                            Toast.makeText(context, "Item berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Nama tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeeklyDialog = false }) {
                    Text("Batal", color = TextGray)
                }
            }
        )
    }

    // Dialog 2: Edit Weekly Item
    if (showEditWeeklyDialog && itemToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditWeeklyDialog = false },
            title = { Text("Ubah Nama Item", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column {
                    Text("Masukkan nama baru untuk item ini:", fontSize = 13.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = weeklyItemName,
                        onValueChange = { weeklyItemName = it },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (weeklyItemName.isNotBlank()) {
                            viewModel.saveMaintenanceItem(outletId, weeklyItemName.trim(), itemToEdit!!.id)
                            showEditWeeklyDialog = false
                            Toast.makeText(context, "Item berhasil diubah.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Nama tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWeeklyDialog = false }) {
                    Text("Batal", color = TextGray)
                }
            }
        )
    }

    // Dialog 3: Delete Weekly Item Confirmation
    if (showDeleteWeeklyDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteWeeklyDialog = false },
            title = { Text("Hapus Item Pemeliharaan", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus item '${itemToDelete!!.name}'? Tindakan ini tidak dapat dibatalkan.",
                    fontSize = 14.sp,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMaintenanceItem(outletId, itemToDelete!!.id)
                        showDeleteWeeklyDialog = false
                        Toast.makeText(context, "Item berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWeeklyDialog = false }) {
                    Text("Batal", color = TextGray)
                }
            }
        )
    }

    // Dialog 4: Record Regulator Test
    if (showRegulatorDialog) {
        AlertDialog(
            onDismissRequest = { showRegulatorDialog = false },
            title = { Text("Catat Tes Regulator Gas", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column {
                    Text("Masukkan tanggal pengujian kebocoran regulator:", fontSize = 13.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = regulatorDay,
                            onValueChange = { input -> regulatorDay = input.filter { it.isDigit() }.take(2) },
                            label = { Text("Tgl (1-31)", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = regulatorMonth,
                            onValueChange = { input -> regulatorMonth = input.filter { it.isDigit() }.take(2) },
                            label = { Text("Bln (1-12)", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = currentYear.toString(),
                        onValueChange = {},
                        label = { Text("Tahun (Kunci Gadget)", fontSize = 12.sp) },
                        enabled = false,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            disabledBorderColor = Color(0xFFE2E8F0),
                            disabledTextColor = TextDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = regulatorDay.toIntOrNull()
                        val m = regulatorMonth.toIntOrNull()
                        if (d != null && d in 1..31 && m != null && m in 1..12) {
                            viewModel.saveRegulatorCheck(outletId, d, m, currentYear)
                            showRegulatorDialog = false
                            Toast.makeText(context, "Tes regulator berhasil dicatat.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Harap masukkan Tanggal (1-31) dan Bulan (1-12) yang valid.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Catat Tes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegulatorDialog = false }) {
                    Text("Batal", color = TextGray)
                }
            }
        )
    }

    // Dialog 5: Edit APAR Check
    if (showAparDialog) {
        AlertDialog(
            onDismissRequest = { showAparDialog = false },
            title = { Text("Ubah Data APAR", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Masukkan data pengisian ulang terakhir dan masa berlaku APAR:", fontSize = 13.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tanggal Terakhir Isi Ulang:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = aparDay,
                            onValueChange = { input -> aparDay = input.filter { it.isDigit() }.take(2) },
                            label = { Text("Tgl (1-31)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = aparMonth,
                            onValueChange = { input -> aparMonth = input.filter { it.isDigit() }.take(2) },
                            label = { Text("Bln (1-12)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1.1f)
                        )

                        OutlinedTextField(
                            value = aparYear,
                            onValueChange = { input -> aparYear = input.filter { it.isDigit() }.take(4) },
                            label = { Text("Thn (4 digit)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Masa Berlaku Pengisian (Bulan):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = aparIntervalMonths,
                        onValueChange = { input -> aparIntervalMonths = input.filter { it.isDigit() }.take(3) },
                        placeholder = { Text("Default: 36") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = aparDay.toIntOrNull()
                        val m = aparMonth.toIntOrNull()
                        val y = aparYear.toIntOrNull()
                        val interval = aparIntervalMonths.toIntOrNull() ?: 36

                        if (d != null && d in 1..31 && m != null && m in 1..12 && y != null && y in 2000..2100) {
                            val cal = Calendar.getInstance()
                            cal.set(y, m - 1, d, 12, 0, 0)
                            viewModel.saveAparCheck(outletId, cal.time, interval)
                            showAparDialog = false
                            Toast.makeText(context, "Data APAR berhasil disimpan.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Harap isi Tanggal (1-31), Bulan (1-12), dan Tahun (4 digit) yang valid.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAparDialog = false }) {
                    Text("Batal", color = TextGray)
                }
            }
        )
    }
}
