package com.prosayac.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prosayac.app.data.datastore.UserPreferences
import com.prosayac.app.presentation.components.GlowingStatusIndicator
import com.prosayac.app.presentation.connection.ConnectionScreen
import com.prosayac.app.presentation.connection.ConnectionViewModel
import com.prosayac.app.presentation.dashboard.DashboardScreen
import com.prosayac.app.presentation.dashboard.DashboardViewModel
import com.prosayac.app.presentation.logs.LogsScreen
import com.prosayac.app.presentation.logs.LogsViewModel
import com.prosayac.app.presentation.meters.MetersScreen
import com.prosayac.app.presentation.meters.MetersViewModel
import com.prosayac.app.presentation.navigation.NavRoute
import com.prosayac.app.presentation.navigation.navItems
import com.prosayac.app.presentation.readings.ReadingsScreen
import com.prosayac.app.presentation.readings.ReadingsViewModel
import com.prosayac.app.presentation.settings.SettingsScreen
import com.prosayac.app.presentation.settings.SettingsViewModel
import com.prosayac.app.presentation.theme.*
import com.prosayac.app.util.serial.ConnectionState
import com.prosayac.app.util.serial.MBusSerialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var serialManager: MBusSerialManager

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by userPreferences.preferencesFlow
                .map { it.themeMode }
                .collectAsStateWithLifecycle("system")

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            ProMaxTheme(darkTheme = darkTheme) {
                ProSayacMainApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        serialManager.reset()
    }

    override fun onStop() {
        super.onStop()
        serialManager.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSayacMainApp(
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ViewModels
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val metersViewModel: MetersViewModel = hiltViewModel()
    val readingsViewModel: ReadingsViewModel = hiltViewModel()
    val logsViewModel: LogsViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val connectionViewModel: ConnectionViewModel = hiltViewModel()

    // Connection state for drawer footer
    val connectionState by connectionViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute = navItems.find { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    } ?: NavRoute.Dashboard

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Drawer Header
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "SAYAÇ PRO",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saha Okuma Sistemi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                navItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            item.icon?.let {
                                Icon(
                                    it,
                                    contentDescription = item.title,
                                    tint = if (currentRoute.route == item.route) ProMaxTertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = {
                            Text(
                                item.title,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = if (currentRoute.route == item.route) FontWeight.Bold
                                else FontWeight.Normal
                            )
                        },
                        selected = currentRoute.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ProMaxTertiaryContainer,
                            selectedTextColor = ProMaxOnTertiaryContainer,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Connection status in drawer footer
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    GlowingStatusIndicator(
                        isConnected = connectionState.connectionState == ConnectionState.CONNECTED,
                        size = 10.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (connectionState.connectionState) {
                            ConnectionState.CONNECTED -> "M-Bus Bağlı"
                            ConnectionState.CONNECTING -> "Bağlanıyor..."
                            else -> "M-Bus Bağlı Değil"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "v1.0.0 · Pro Max",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(28.dp)
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = NavRoute.Dashboard.route
        ) {
            composable(NavRoute.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToMeters = {
                        navController.navigate(NavRoute.Meters.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(NavRoute.Meters.route) {
                MetersScreen(
                    viewModel = metersViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(NavRoute.Readings.route) {
                ReadingsScreen(
                    viewModel = readingsViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(NavRoute.Logs.route) {
                LogsScreen(
                    viewModel = logsViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(NavRoute.Connection.route) {
                ConnectionScreen(
                    viewModel = connectionViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}
