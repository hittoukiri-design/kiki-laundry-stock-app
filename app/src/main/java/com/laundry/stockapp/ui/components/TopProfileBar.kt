package com.laundry.stockapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.laundry.stockapp.ui.theme.PrimaryBlue
import com.laundry.stockapp.ui.theme.SecondaryTeal
import com.laundry.stockapp.ui.theme.TextGray

@Composable
fun TopProfileBar(
    userName: String,
    profileImageUri: String?,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Halo, $userName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = "Selamat datang kembali!",
                fontSize = 14.sp,
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        var showProfileMenu by remember { mutableStateOf(false) }
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SecondaryTeal)
                    .clickable { 
                        if (onLogout != null) {
                            showProfileMenu = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White
                    )
                }
            }
            if (onLogout != null) {
                DropdownMenu(
                    expanded = showProfileMenu,
                    onDismissRequest = { showProfileMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        onClick = {
                            showProfileMenu = false
                            onLogout()
                        }
                    )
                }
            }
        }
    }
}
