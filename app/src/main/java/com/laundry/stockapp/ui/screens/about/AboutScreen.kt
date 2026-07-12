// This App was build by Chris Tambayong - Fumakill4
package com.laundry.stockapp.ui.screens.about

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.stockapp.R
import com.laundry.stockapp.ui.theme.PrimaryBlue
import com.laundry.stockapp.ui.theme.SecondaryTeal
import com.laundry.stockapp.ui.theme.TextDark
import com.laundry.stockapp.ui.theme.TextGray

@Composable
fun AboutScreen(
    userName: String = "Pengguna",
    profileImageUri: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB)) // BackgroundLight color
    ) {
        // Decorative Header with towels wave decoration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.White)
        ) {
            // Wave backdrop drawn via Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width * 0.45f, 0f)
                    cubicTo(
                        size.width * 0.55f, size.height * 0.4f,
                        size.width * 0.70f, size.height * 0.1f,
                        size.width, size.height * 0.9f
                    )
                    close()
                }
                
                // Draw wave with soft gradient matching the mockup
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE6F7FA).copy(alpha = 0.5f),
                            Color(0xFFE2F4F7)
                        )
                    )
                )

                // Extra bubbles drawn directly via Canvas
                drawCircle(
                    color = Color(0xFFCBEFF2).copy(alpha = 0.4f),
                    radius = 12.dp.toPx(),
                    center = Offset(size.width * 0.72f, size.height * 0.28f)
                )
                drawCircle(
                    color = Color(0xFFD3F4F7).copy(alpha = 0.5f),
                    radius = 8.dp.toPx(),
                    center = Offset(size.width * 0.76f, size.height * 0.42f)
                )
                drawCircle(
                    color = Color(0xFFDFF6F8).copy(alpha = 0.4f),
                    radius = 5.dp.toPx(),
                    center = Offset(size.width * 0.81f, size.height * 0.22f)
                )
            }

            // Towels image positioned in the top-right corner
            Image(
                painter = painterResource(id = R.drawable.towels_wave_decoration),
                contentDescription = "Towel Stack",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(160.dp)
                    .offset(x = 10.dp, y = (-20).dp),
                contentScale = ContentScale.Fit
            )

            // Header titles
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = "About",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Informasi aplikasi dan dukungan",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }
        }

        // Main content inside vertical scroll to ensure compatibility on various screen heights
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Left (Large and blending directly with card background)
                    Image(
                        painter = painterResource(id = R.drawable.londri_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(130.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    // Text Info Right
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Styled app name: "Admin User - Stock App"
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                                        append("Admin User ")
                                    }
                                    withStyle(style = SpanStyle(color = SecondaryTeal, fontWeight = FontWeight.Bold)) {
                                        append("- Stock App")
                                    }
                                },
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // v1.0 badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF3E8FF))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "v.$versionName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5CF6)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Aplikasi ini membantu admin operasional laundry mengelola master item, stok awal, pengambilan barang outlet, dan riwayat stok dengan cepat dan mudah.",
                            fontSize = 13.sp,
                            color = TextGray,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Bottom Grid: 4 Cards in a Row with equal heights
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Fitur Utama
                InfoCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "Fitur Utama",
                    icon = Icons.Default.StarBorder,
                    iconTint = SecondaryTeal,
                    iconBg = Color(0xFFEBF8FA)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val features = listOf("Master Item", "Stok Awal", "Pengambilan Outlet", "Riwayat Stok", "Export Excel")
                        features.forEach { feature ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981), // Green checkmark
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = feature,
                                    fontSize = 11.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Card 2: Informasi Aplikasi
                InfoCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "Informasi Aplikasi",
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFF0284C7),
                    iconBg = Color(0xFFE0F2FE)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppInfoRow(label = "Versi", value = "V.$versionName")
                        AppInfoRow(label = "Platform", value = "Android Tablet")
                        AppInfoRow(label = "Paket", value = "com.laundry.stockapp")
                        AppInfoRow(label = "Mode", value = "Landscape")
                    }
                }

                // Card 3: Bantuan & Dukungan
                InfoCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "Bantuan & Dukungan",
                    icon = Icons.Default.HeadsetMic,
                    iconTint = Color(0xFF8B5CF6),
                    iconBg = Color(0xFFF3E8FF)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppInfoRow(label = "Pemilik Aplikasi", value = "KRISTIE KIKI")
                        AppInfoRow(label = "Support", value = "Suami nya kiki")
                        
                        Text(
                            text = "Status Sinkronisasi",
                            fontSize = 10.sp,
                            color = TextGray
                        )
                        
                        // Firebase Connected Pill Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDCFCE7))
                                .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Firebase Connected",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                // Card 4: Tentang Brand (with custom leaf background motif)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Leaf Watermark Image (Matching user's mockup leaf layout)
                        Image(
                            painter = painterResource(id = R.drawable.leaf_watermark),
                            contentDescription = "Leaf Watermark",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(110.dp)
                                .offset(x = 10.dp, y = 10.dp),
                            contentScale = ContentScale.Fit,
                            alpha = 0.5f // Make it soft and transparent like a watermark
                        )

                        // Card Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Circular icon badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFE0F2FE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Tentang Aplikasi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "KRISTIE KIKI menghadirkan aplikasi sederhana, rapi, dan ringan untuk membantu kontrol stok outlet laundry sehari-hari.",
                                fontSize = 11.sp,
                                color = TextGray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons Row (Cek Pembaruan & Hubungi Admin)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Button 1: Cek Pembaruan
                Button(
                    onClick = {
                        Toast.makeText(context, "Aplikasi Anda sudah versi terbaru (v.$versionName).", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .wrapContentWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cek Pembaruan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Button 2: Hubungi Admin
                Button(
                    onClick = {
                        Toast.makeText(context, "Membuka obrolan dukungan administrator...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)), // Purple/lavender color
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .wrapContentWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hubungi Admin",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Circular icon badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            content()
        }
    }
}

@Composable
fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextGray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
    }
}
