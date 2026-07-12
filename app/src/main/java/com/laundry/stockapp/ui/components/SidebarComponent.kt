package com.laundry.stockapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.stockapp.R
import com.laundry.stockapp.ui.theme.PrimaryBlue
import com.laundry.stockapp.ui.theme.TextGray

data class MenuItem(
    val route: String,
    val title: String,
    val iconVector: ImageVector? = null,
    val iconRes: Int? = null
)

@Composable
fun SidebarComponent(
    currentRoute: String,
    userName: String,
    userRole: String,
    profileImageUri: String?,
    onNavigate: (String) -> Unit
) {
    val menuItems = listOf(
        MenuItem("dashboard", "Dashboard", iconVector = Icons.Default.Home),
        MenuItem("master_item", "Master Item", iconVector = Icons.Default.Inventory2),
        MenuItem("input_transaction", "Input Transaksi", iconVector = Icons.Default.PostAdd),
        MenuItem("outlet_list", "Daftar Outlet", iconVector = Icons.Default.Storefront),
        MenuItem("maintenance_list", "Ceklist & Pemeliharaan", iconVector = Icons.Default.Build),
        MenuItem("export", "Export Excel", iconRes = R.drawable.excel_logo),
        MenuItem("backup", "Backup Drive", iconVector = Icons.Default.CloudUpload),
        MenuItem("settings", "Settings", iconVector = Icons.Default.Settings),
        MenuItem("about", "About", iconVector = Icons.Default.Info)
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Color.White)
    ) {
        // Logo
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.londri_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu List (Scrollable to prevent cutoff on smaller screens)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            menuItems.forEach { item ->
                val isSelected = currentRoute == item.route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryBlue else Color.Transparent)
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.iconVector != null) {
                        Icon(
                            imageVector = item.iconVector,
                            contentDescription = item.title,
                            tint = if (isSelected) Color.White else PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (item.iconRes != null) {
                        Image(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp),
                            colorFilter = if (item.route == "export") null else androidx.compose.ui.graphics.ColorFilter.tint(if (isSelected) Color.White else PrimaryBlue)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = item.title,
                        color = if (isSelected) Color.White else PrimaryBlue,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Profile Section at the bottom
        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            // Wavy background with motif circles
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, 30.dp.toPx())
                    cubicTo(
                        size.width * 0.25f, -10.dp.toPx(),
                        size.width * 0.65f, 45.dp.toPx(),
                        size.width, 25.dp.toPx()
                    )
                    lineTo(size.width, size.height)
                    close()
                }
                
                // Draw wavy background with gradient
                drawPath(
                    path = path,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5EFFF),
                            Color(0xFFE8DCFF)
                        )
                    )
                )

                // Draw motif circles (bubbles)
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 8.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.45f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.74f, size.height * 0.32f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.65f)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onNavigate("profile") }
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        Image(
                            painter = coil.compose.rememberAsyncImagePainter(profileImageUri),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = PrimaryBlue
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = userName.ifEmpty { "Admin" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = userRole.ifEmpty { "Admin" },
                        fontSize = 12.sp,
                        color = PrimaryBlue.copy(alpha=0.7f)
                    )
                }
            }
        }
    }
}
