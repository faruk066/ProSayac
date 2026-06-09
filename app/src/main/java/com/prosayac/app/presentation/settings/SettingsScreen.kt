package com.prosayac.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prosayac.app.presentation.components.DangerZoneCard
import com.prosayac.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Genel", "Bağlantı", "Veri", "Güvenlik")
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBaudRateDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Ayarlar", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menü") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ProMaxTertiary,
                edgePadding = 16.dp,
                divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        selectedContentColor = ProMaxTertiary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> GenelTab(state.themeMode, state.autoSyncEnabled, state.offlineMode,
                        onThemeClick = { showThemeDialog = true },
                        onAutoSyncToggle = { viewModel.updateAutoSync(it) },
                        onOfflineToggle = { viewModel.updateOfflineMode(it) }
                    )
                    1 -> BaglantiTab(state.baudRate,
                        onBaudRateClick = { showBaudRateDialog = true }
                    )
                    2 -> VeriTab(onResetData = { showResetDialog = true })
                    3 -> GuvenlikTab()
                }

                Spacer(modifier = Modifier.height(32.dp))

                // App footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sayaç Pro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("v1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Saha Sayac Okuma Uygulaması", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Theme dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Tema Seçin", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("light" to "Açık", "dark" to "Koyu", "system" to "Sistem").forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.themeMode == value, onClick = {
                                viewModel.updateThemeMode(value)
                                showThemeDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("İptal") } }
        )
    }

    // Baud rate dialog
    if (showBaudRateDialog) {
        val baudRates = listOf(1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200)
        AlertDialog(
            onDismissRequest = { showBaudRateDialog = false },
            title = { Text("Baud Rate Seçin", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    baudRates.forEach { rate ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.baudRate == rate, onClick = {
                                viewModel.updateBaudRate(rate)
                                showBaudRateDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$rate bps", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBaudRateDialog = false }) { Text("İptal") } }
        )
    }

    // Reset dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Verileri Sıfırla", fontWeight = FontWeight.Bold, color = ProMaxError) },
            text = { Text("Tüm sayaçlar ve okuma kayıtları kalıcı olarak silinecektir. Bu işlem geri alınamaz!") },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("İptal") }
            },
dismissButton = {
                        TextButton(onClick = { 
                            viewModel.deleteAllData()
                            showResetDialog = false 
                        }) {
                            Text("Tümünü Sil", color = ProMaxError)
                        }
                    }
        )
    }
}

// =============================================================================
// TAB 1: GENEL
// =============================================================================
@Composable
fun GenelTab(
    themeMode: String,
    autoSyncEnabled: Boolean,
    offlineMode: Boolean,
    onThemeClick: () -> Unit,
    onAutoSyncToggle: (Boolean) -> Unit,
    onOfflineToggle: (Boolean) -> Unit
) {
    Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("GENEL", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Divider()
            ListItem(
                headlineContent = { Text("Tema") },
                supportingContent = { Text(when (themeMode) { "light" -> "Açık" ; "dark" -> "Koyu" ; else -> "Sistem" }) },
                leadingContent = { Icon(Icons.Default.Palette, null, tint = ProMaxTertiary) },
                modifier = Modifier
                    .padding(0.dp)
                    .clickable { onThemeClick() }
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text("Otomatik Senkronizasyon") },
                supportingContent = { Text(if (autoSyncEnabled) "Açık" else "Kapalı") },
                leadingContent = { Icon(Icons.Default.Sync, null, tint = SyncSynced) },
                trailingContent = { Switch(checked = autoSyncEnabled, onCheckedChange = onAutoSyncToggle) }
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text("Çevrimdışı Mod") },
                supportingContent = { Text(if (offlineMode) "Aktif" else "Pasif") },
                leadingContent = { Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingContent = { Switch(checked = offlineMode, onCheckedChange = onOfflineToggle) }
            )
        }
    }
}

// =============================================================================
// TAB 2: BAĞLANTI
// =============================================================================
@Composable
fun BaglantiTab(
    baudRate: Int,
    onBaudRateClick: () -> Unit
) {
    Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("BAĞLANTI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Divider()
            ListItem(
                headlineContent = { Text("USB-OTG M-Bus") },
                supportingContent = {
                    Text("FTDI, CP210x, CH340, PL2303 seri dönüştürücüler desteklenir")
                },
                leadingContent = { Icon(Icons.Default.Usb, null, tint = ProMaxTertiary) }
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text("Baud Rate") },
                supportingContent = { Text("$baudRate bps") },
                leadingContent = { Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text("Veri Bitleri") },
                supportingContent = { Text("8 bit") },
                leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onBaudRateClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = ProMaxTertiary)
    ) {
        Icon(Icons.Default.Speed, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Baud Rate Ayarla")
    }
}

// =============================================================================
// TAB 3: VERI (DATA)
// =============================================================================
@Composable
fun VeriTab(
    onResetData: () -> Unit
) {
    Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("VERİ YÖNETİMİ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Divider()
            ListItem(
                headlineContent = { Text("Veritabanı Yolu") },
                supportingContent = { Text("prosayac_database", fontFamily = FontFamily.Monospace) },
                leadingContent = { Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            ListItem(
                headlineContent = { Text("Oda Sürümü") },
                supportingContent = { Text("v2") },
                leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    DangerZoneCard(
        title = "TEHLİKE BÖLGESİ",
        description = "Tüm sayaç verilerini, okuma kayıtlarını ve içe aktarılan tüm verileri kalıcı olarak siler. Bu işlem geri alınamaz!",
        actionLabel = "TÜM VERİLERİ SIFIRLA",
        onAction = onResetData
    )
}

// =============================================================================
// TAB 4: GÜVENLİK
// =============================================================================
@Composable
fun GuvenlikTab() {
    Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("GÜVENLİK", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Divider()
            ListItem(
                headlineContent = { Text("Veri Depolama") },
                supportingContent = { Text("Tüm veriler cihazda yerel olarak saklanır. Harici sunuculara yüklenmez.") },
                leadingContent = { Icon(Icons.Default.Lock, null, tint = SyncSynced) }
            )
            ListItem(
                headlineContent = { Text("Erişim İzinleri") },
                supportingContent = { Text("USB, Depolama alanı okuma") },
                leadingContent = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }
    }
}