// This App was build by Chris Tambayong - Fumakill4
package com.laundry.stockapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import com.laundry.stockapp.ui.theme.LaundryStockAppTheme
import com.laundry.stockapp.ui.screens.dashboard.DashboardScreen
import com.laundry.stockapp.ui.screens.item.MasterItemScreen
import com.laundry.stockapp.ui.screens.transaction.InputTransactionScreen
import com.laundry.stockapp.ui.screens.outlet.OutletListScreen
import com.laundry.stockapp.ui.screens.outlet.OutletDetailScreen
import com.laundry.stockapp.ui.screens.outlet.MaintenanceListScreen
import com.laundry.stockapp.ui.screens.settings.SettingsScreen
import com.laundry.stockapp.ui.components.SidebarComponent
import com.laundry.stockapp.ui.components.AppLockOverlay
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.laundry.stockapp.data.repository.DataSeeder
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataSeeder: DataSeeder

    @Inject
    lateinit var settingsRepository: com.laundry.stockapp.data.repository.SettingsRepository

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide status bar dynamically (auto-hide / immersive fullscreen)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
        
        viewModel = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        
        lifecycleScope.launch {
            dataSeeder.seedDatabaseIfEmpty()
            try {
                val enabled = settingsRepository.backupEnabled.firstOrNull() ?: false
                if (enabled) {
                    com.laundry.stockapp.util.BackupManager.schedulePeriodicBackup(applicationContext)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            LaundryStockAppTheme(darkTheme = false) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)
                val userName by viewModel.userName.collectAsState(initial = "Pengguna")
                val userRole by viewModel.userRole.collectAsState(initial = "Admin")
                val profilePhone by viewModel.profilePhone.collectAsState(initial = "-")
                val profileImageUri by viewModel.profileImageUri.collectAsState(initial = null)
                val isLocked by viewModel.isLocked.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isLoggedIn) {
                        com.laundry.stockapp.ui.screens.login.LoginScreen(onLoginSuccess = {})
                    } else {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Sidebar Left
                                SidebarComponent(
                                    currentRoute = currentRoute,
                                    userName = userName ?: "Pengguna",
                                    userRole = userRole ?: "Admin",
                                    profileImageUri = profileImageUri,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )

                                // Content Right
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .background(com.laundry.stockapp.ui.theme.BackgroundLight)
                                  ) {
                                      NavHost(
                                          navController = navController,
                                          startDestination = "dashboard",
                                          modifier = Modifier.fillMaxSize()
                                      ) {
                                          composable("dashboard") { DashboardScreen(navController, profileImageUri = profileImageUri) }
                                          composable("master_item") { MasterItemScreen(userName = userName ?: "Pengguna", profileImageUri = profileImageUri) }
                                          composable("input_transaction") { 
                                              InputTransactionScreen(
                                                  userName = userName ?: "Pengguna", 
                                                  profileImageUri = profileImageUri,
                                                  onNavigateToHistory = { navController.navigate("transaction_history") }
                                              ) 
                                          }
                                          composable("transaction_history") {
                                              com.laundry.stockapp.ui.screens.transaction.TransactionHistoryScreen(
                                                  onBack = { navController.popBackStack() },
                                                  userName = userName ?: "Pengguna",
                                                  profileImageUri = profileImageUri
                                              )
                                          }
                                          composable("outlet_list") {
                                              OutletListScreen(
                                                  userName = userName ?: "Pengguna",
                                                  profileImageUri = profileImageUri,
                                                  onNavigateDetail = { outletId ->
                                                      navController.navigate("outlet_detail/$outletId?tab=0")
                                                  }
                                              )
                                          }
                                          composable("maintenance_list") {
                                              MaintenanceListScreen(
                                                  userName = userName ?: "Pengguna",
                                                  profileImageUri = profileImageUri,
                                                  onNavigateDetail = { outletId ->
                                                      navController.navigate("outlet_detail/$outletId?tab=1")
                                                  }
                                              )
                                          }
                                          composable(
                                              route = "outlet_detail/{outletId}?tab={tab}",
                                              arguments = listOf(
                                                  navArgument("outletId") { type = NavType.StringType },
                                                  navArgument("tab") {
                                                      type = NavType.IntType
                                                      defaultValue = 0
                                                  }
                                              )
                                          ) { backStackEntry ->
                                              val outletId = backStackEntry.arguments?.getString("outletId") ?: ""
                                              val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                                              OutletDetailScreen(
                                                  outletId = outletId,
                                                  initialTab = initialTab,
                                                  onBack = { navController.popBackStack() }
                                              )
                                          }
                                        composable("export") { com.laundry.stockapp.ui.screens.export.ExportExcelScreen(userName = userName ?: "Pengguna", profileImageUri = profileImageUri) }
                                        composable("backup") { 
                                            com.laundry.stockapp.ui.screens.backup.BackupDriveScreen(
                                                userName = userName ?: "Pengguna", 
                                                profileImageUri = profileImageUri,
                                                onNavigateHistory = { navController.navigate("backup_history") }
                                            ) 
                                        }
                                        composable("backup_history") { 
                                            com.laundry.stockapp.ui.screens.backup.BackupHistoryScreen(
                                                onBack = { navController.popBackStack() },
                                                userName = userName ?: "Pengguna",
                                                profileImageUri = profileImageUri
                                            ) 
                                        }
                                        composable("settings") { SettingsScreen(userName = userName ?: "Pengguna", profileImageUri = profileImageUri) }
                                        composable("about") {
                                            com.laundry.stockapp.ui.screens.about.AboutScreen(
                                                userName = userName ?: "Pengguna",
                                                profileImageUri = profileImageUri
                                            )
                                        }
                                        composable("profile") { 
                                            com.laundry.stockapp.ui.screens.profile.ProfileScreen(
                                                userName = userName ?: "Pengguna",
                                                userRole = userRole ?: "Admin",
                                                initialPhone = profilePhone ?: "-",
                                                initialImageUri = profileImageUri,
                                                onSave = { phone, uri -> viewModel.saveProfile(phone, uri) },
                                                onBack = { navController.popBackStack() }
                                            ) 
                                        }
                                    }
                                }
                            }

                            // Floating Action Button for manual lock/unlock
                            val isLockEnabledState by viewModel.isAppLockEnabled.collectAsState(initial = false)
                            val lockPinState by viewModel.appLockPin.collectAsState(initial = null)

                            if (isLockEnabledState && !lockPinState.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.BottomEnd
                                ) {
                                    FloatingActionButton(
                                        onClick = {
                                            if (!isLocked) {
                                                viewModel.lockManually()
                                            }
                                        },
                                        containerColor = if (isLocked) Color(0xFFEF4444) else com.laundry.stockapp.ui.theme.PrimaryBlue,
                                        contentColor = Color.White,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = if (isLocked) "Terkunci" else "Buka Kunci"
                                        )
                                    }
                                }
                            }

                            // Lock Screen Overlay
                            if (isLocked) {
                                AppLockOverlay(
                                    onUnlock = { pin ->
                                        var success = false
                                        viewModel.unlock(
                                            pin = pin,
                                            onSuccess = { success = true },
                                            onError = {}
                                        )
                                        success
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::viewModel.isInitialized) {
            viewModel.updateInteraction()
        }
    }
}
