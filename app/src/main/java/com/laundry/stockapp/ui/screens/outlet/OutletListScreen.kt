package com.laundry.stockapp.ui.screens.outlet

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laundry.stockapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletListScreen(
    userName: String = "",
    profileImageUri: String? = null,
    onNavigateDetail: (String) -> Unit = {},
    viewModel: OutletViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Semua Wilayah") }
    val tabs = listOf("Semua Wilayah", "Selatan", "Utara", "Barat")
    val state by viewModel.listState.collectAsState()

    // Fix #2: Dynamic counts from state
    val selatanCount = state.outletsByRegion["Selatan"]?.size ?: 0
    val utaraCount   = state.outletsByRegion["Utara"]?.size   ?: 0
    val baratCount   = state.outletsByRegion["Barat"]?.size   ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Fix #1: Header padding 20dp top/start/end, 12dp bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    // Fix #1: title 24sp bold blue
                    Text(
                        text = "Daftar Outlet",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kelola dan lihat daftar outlet berdasarkan wilayah",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }

                // Fix #9: RegionCard width 120dp, height 60dp
                // Fix #2: counts are dynamic strings from state
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RegionCard(
                        title = "Selatan",
                        count = selatanCount.toString(),
                        iconColor = SecondaryTeal,
                        bgColor = Color(0xFFE0F2FE),
                        icon = Icons.Default.HolidayVillage
                    )
                    RegionCard(
                        title = "Utara",
                        count = utaraCount.toString(),
                        iconColor = Color(0xFF6366F1),
                        bgColor = Color(0xFFF3E8FF),
                        icon = Icons.Default.Terrain
                    )
                    RegionCard(
                        title = "Barat",
                        count = baratCount.toString(),
                        iconColor = Color(0xFFD97706),
                        bgColor = Color(0xFFFEF3C7),
                        icon = Icons.Default.Deck
                    )
                }
            }
        }

        // Fix #6: LazyColumn padding 20dp horizontal
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Search bar + tab pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
                        },
                        placeholder = { Text("Cari outlet...", color = TextGray, fontSize = 13.sp) },
                        modifier = Modifier
                            .width(280.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                            .padding(4.dp)
                    ) {
                        tabs.forEach { tab ->
                            val isSel = selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) PrimaryBlue else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSel) Color.White else TextGray,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Fix #5: Tab-filtered columns
            item {
                val visibleRegions = when (selectedTab) {
                    "Selatan" -> listOf("Selatan")
                    "Utara"   -> listOf("Utara")
                    "Barat"   -> listOf("Barat")
                    else      -> listOf("Selatan", "Utara", "Barat")
                }

                data class RegionMeta(
                    val icon: androidx.compose.ui.graphics.vector.ImageVector,
                    val color: Color,
                    val bgColor: Color
                )

                val regionMeta = mapOf(
                    "Selatan" to RegionMeta(Icons.Default.HolidayVillage, SecondaryTeal,     Color(0xFFE0F2FE)),
                    "Utara"   to RegionMeta(Icons.Default.Terrain,         Color(0xFF6366F1), Color(0xFFF3E8FF)),
                    "Barat"   to RegionMeta(Icons.Default.Deck,            Color(0xFFD97706), Color(0xFFFEF3C7))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    visibleRegions.forEach { region ->
                        val meta         = regionMeta[region]!!
                        // Fix #3: Dynamic count in column header
                        val regionCount  = state.outletsByRegion[region]?.size ?: 0
                        // Fix #4: Real outlet names; Fix search filter integrated
                        val outlets      = (state.outletsByRegion[region] ?: emptyList())
                            .filter {
                                searchQuery.isBlank() ||
                                it.name.orEmpty().contains(searchQuery, ignoreCase = true)
                            }

                        Column(modifier = Modifier.weight(1f)) {
                            // Column header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        meta.icon,
                                        contentDescription = null,
                                        tint = meta.color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = region,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PrimaryBlue
                                    )
                                }
                                Text(
                                    text = "$regionCount Outlet",
                                    fontSize = 11.sp,
                                    color = meta.color,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Fix #4 + #8: Outlet cards with real names and dynamic stats
                            outlets.forEach { outlet ->
                                val stats = state.outletStats[outlet.id] ?: Pair(0, 0)
                                OutletCard(
                                    name = outlet.name.orEmpty(),
                                    region = region,
                                    qty = stats.first.toString(),
                                    trans = stats.second.toString(),
                                    iconColor = meta.color,
                                    onClick = {
                                        onNavigateDetail(outlet.id)
                                    }
                                )
                                // Fix #7: 8dp spacer between cards
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// Fix #9: RegionCard width 120dp, height 60dp
@Composable
fun RegionCard(
    title: String,
    count: String,
    iconColor: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(60.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Medium)
                Text(count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Text("Total Outlet", fontSize = 8.sp, color = TextGray)
            }
        }
    }
}

// Fix #5 + #8: OutletCard wrap-content, onClick parameter
@Composable
fun OutletCard(
    name: String,
    region: String,
    qty: String,
    trans: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                Text(region, fontSize = 9.sp, color = TextGray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(qty, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = iconColor)
                Text("Total Qty Keluar", fontSize = 8.sp, color = TextGray)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(trans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                Text("Transaksi", fontSize = 8.sp, color = TextGray)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
