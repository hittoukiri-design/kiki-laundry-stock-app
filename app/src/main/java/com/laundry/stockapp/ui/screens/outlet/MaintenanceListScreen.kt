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
fun MaintenanceListScreen(
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

    val selatanCount = state.outletsByRegion["Selatan"]?.size ?: 0
    val utaraCount   = state.outletsByRegion["Utara"]?.size   ?: 0
    val baratCount   = state.outletsByRegion["Barat"]?.size   ?: 0

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Ceklist & Pemeliharaan",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pantau kelayakan mesin, regulator gas, dan masa aktif tabung APAR",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
                
                // Profile Bar
                com.laundry.stockapp.ui.components.TopProfileBar(
                    userName = userName,
                    profileImageUri = profileImageUri
                )
            }
        }

        // Search and Tabs row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari outlet...", color = TextGray, fontSize = 13.sp) },
                    prefix = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = PrimaryBlue
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))

                // Region Tab Pills
                tabs.forEach { tab ->
                    val isTabSelected = selectedTab == tab
                    val countText = when (tab) {
                        "Selatan" -> " ($selatanCount)"
                        "Utara" -> " ($utaraCount)"
                        "Barat" -> " ($baratCount)"
                        else -> ""
                    }
                    
                    Surface(
                        onClick = { selectedTab = tab },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTabSelected) PrimaryBlue else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isTabSelected) PrimaryBlue else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$tab$countText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTabSelected) Color.White else TextDark
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            val regionsToShow = when (selectedTab) {
                "Selatan" -> listOf("Selatan")
                "Utara"   -> listOf("Utara")
                "Barat"   -> listOf("Barat")
                else      -> listOf("Selatan", "Utara", "Barat")
            }

            // Filtered outlets list
            val filteredOutlets = state.outlets.filter {
                it.name.orEmpty().contains(searchQuery, ignoreCase = true) &&
                        regionsToShow.contains(it.region.orEmpty())
            }

            if (filteredOutlets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada outlet ditemukan", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredOutlets.size) { index ->
                        val outlet = filteredOutlets[index]
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Membuka pemeliharaan ${outlet.name}...", Toast.LENGTH_SHORT).show()
                                    onNavigateDetail(outlet.id)
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Shop Icon Circle
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFEFF6FF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(
                                            text = outlet.name.orEmpty(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Wilayah: ${outlet.region.orEmpty()}",
                                            fontSize = 12.sp,
                                            color = TextGray
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF0FDF4),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "Cek Status",
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Buka Detail",
                                        tint = TextGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
