package com.laundry.stockapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockOverlay(
    onUnlock: suspend (String) -> Boolean
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60F172A)) // Semi-transparent dark slate
            .clickable(enabled = false) {}, // Intercept click events
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowy lock icon header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(if (isError) Color(0xFFFEE2E2) else Color(0xFFE0F2FE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = if (isError) Color(0xFFEF4444) else Color(0xFF0284C7),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Aplikasi Terkunci",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isError) errorMessage else "Masukkan 6-digit PIN Keamanan Anda",
                    fontSize = 13.sp,
                    color = if (isError) Color(0xFFEF4444) else Color(0xFF64748B),
                    fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN dots indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (isError) {
                                        Color(0xFFEF4444)
                                    } else if (isFilled) {
                                        Color(0xFF0284C7)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isError) {
                                        Color(0xFFEF4444)
                                    } else if (isFilled) {
                                        Color(0xFF0284C7)
                                    } else {
                                        Color(0xFFCBD5E1)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Numeric Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "delete")
                    )

                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            for (key in row) {
                                KeypadButton(
                                    text = if (key == "delete") "" else key,
                                    icon = if (key == "delete") Icons.Default.Backspace else null,
                                    onClick = {
                                        if (isError) {
                                            isError = false
                                        }

                                        when (key) {
                                            "C" -> {
                                                enteredPin = ""
                                            }
                                            "delete" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 6) {
                                                    enteredPin += key
                                                    if (enteredPin.length == 6) {
                                                        // Auto trigger unlock verification
                                                        scope.launch {
                                                            val success = onUnlock(enteredPin)
                                                            if (!success) {
                                                                isError = true
                                                                errorMessage = "PIN Salah! Silakan coba lagi."
                                                                enteredPin = ""
                                                                delay(1500)
                                                                isError = false
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = "Backspace",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )
        }
    }
}
