package com.laundry.stockapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.stockapp.ui.theme.PrimaryBlue
import com.laundry.stockapp.ui.theme.SecondaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    userRole: String,
    initialPhone: String,
    initialImageUri: String?,
    onSave: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Header - Static
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profil Saya",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                        var imageUri by androidx.compose.runtime.remember(initialImageUri) { 
                            androidx.compose.runtime.mutableStateOf(initialImageUri?.let { android.net.Uri.parse(it) }) 
                        }
                        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.GetContent()
                        ) { uri: android.net.Uri? ->
                            imageUri = uri
                        }

                // Profile Photo Editable
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    if (imageUri != null) {
                        coil.compose.AsyncImage(
                            model = imageUri,
                            contentDescription = "Profile Photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(60.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(60.dp))
                                .background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Photo",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                    
                    // Camera Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SecondaryTeal)
                            .clickable { 
                                launcher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ganti Foto",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                var currentName by androidx.compose.runtime.remember(userName) { androidx.compose.runtime.mutableStateOf(userName.ifEmpty { "Pengguna" }) }
                var currentEmail by androidx.compose.runtime.remember(userName) { androidx.compose.runtime.mutableStateOf("") } // TODO: get from AuthRepository or ViewModel
                var currentPhone by androidx.compose.runtime.remember(initialPhone) { androidx.compose.runtime.mutableStateOf(initialPhone) }
                var showSuccess by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                if (showSuccess) {
                    AlertDialog(
                        onDismissRequest = { showSuccess = false },
                        title = { Text("Sukses", color = SecondaryTeal) },
                        text = { Text("Profil berhasil diperbarui.") },
                        confirmButton = {
                            Button(onClick = { showSuccess = false }) { Text("OK") }
                        }
                    )
                }

                // Name
                OutlinedTextField(
                    value = currentName,
                    onValueChange = { currentName = it },
                    label = { Text("Nama Lengkap") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email
                OutlinedTextField(
                    value = currentEmail,
                    onValueChange = { currentEmail = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone
                OutlinedTextField(
                    value = currentPhone,
                    onValueChange = { currentPhone = it },
                    label = { Text("Nomor HP") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        onSave(currentPhone, imageUri?.toString())
                        showSuccess = true 
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("SIMPAN PERUBAHAN", fontWeight = FontWeight.Bold)
                }
            }
        }
        }
    }
}
