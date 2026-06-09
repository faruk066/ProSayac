package com.prosayac.app.presentation.connection

import androidx.compose.animation.*
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
import com.prosayac.app.presentation.components.GlowingStatusIndicator
import com.prosayac.app.presentation.theme.*
import com.prosayac.app.util.serial.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBaudRateDialog by remember { mutableStateOf(false) }

    val isConnected = state.connectionState == ConnectionState.CONNECTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("M-Bus Bağlantı", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menü") }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshState() }) {
                        Icon(Icons.Default.Refresh, "Yenile")
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== MAIN STATUS CARD =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = when (state.connectionState) {
                        ConnectionState.CONNECTED -> SyncSynced.copy(alpha = 0.08f)
                        ConnectionState.CONNECTING -> ChartOrange.copy(alpha = 0.08f)
                        ConnectionState.ERROR -> ProMaxError.copy(alpha = 0.08f)
                        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = if (isConnected) 2.dp else 1.dp,
                    brush = if (isConnected) {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(SyncSynced, SyncSynced.copy(alpha = 0.3f)))
                    } else {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated glowing indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlowingStatusIndicator(
                            isConnected = isConnected,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            when (state.connectionState) {
                                ConnectionState.CONNECTED -> "Bağlı"
                                ConnectionState.CONNECTING -> "Bağlanıyor..."
                                ConnectionState.ERROR -> "Bağlantı Hatası"
                                ConnectionState.DISCONNECTED -> "Bağlantı Yok"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when (state.connectionState) {
                                ConnectionState.CONNECTED -> SyncSynced
                                ConnectionState.CONNECTING -> ChartOrange
                                ConnectionState.ERROR -> ProMaxError
                                ConnectionState.DISCONNECTED -> ProMaxError
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        when (state.connectionState) {
                            ConnectionState.CONNECTED -> "USB-OTG M-Bus ağ geçidi bağlı ve çalışıyor"
                            ConnectionState.CONNECTING -> "Cihaz algılanıyor, bağlantı kuruluyor..."
                            ConnectionState.ERROR -> "Bağlantı sırasında bir hata oluştu"
                            ConnectionState.DISCONNECTED -> "M-Bus donanımı bağlı değil veya algılanamadı"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    AnimatedVisibility(state.isConnecting) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ProMaxTertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bağlantı kontrol ediliyor...", style = MaterialTheme.typography.bodySmall, color = ChartOrange)
                        }
                    }
                }
            }

            // ===== BAUD RATE SELECTOR =====
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Baud Rate") },
                        supportingContent = { Text("${state.baudRate} bps") },
                        leadingContent = { Icon(Icons.Default.Speed, null, tint = ProMaxTertiary) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(
                        onClick = { showBaudRateDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Baud Rate Değiştir")
                    }
                }
            }

            // ===== CONNECT / DISCONNECT BUTTON =====
            Button(
                onClick = {
                    if (state.connectionState == ConnectionState.CONNECTED) {
                        viewModel.disconnect()
                    } else {
                        viewModel.connect(state.baudRate)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.connectionState != ConnectionState.CONNECTING,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) ProMaxError else SyncSynced
                )
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.Usb,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isConnected) "BAĞLANTIYI KES" else "BAĞLAN",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // ===== HARDWARE INFO =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DONANIM BİLGİSİ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "USB-OTG kablosu ile M-Bus GSM/GPRS ağ geçidini bağlayın. " +
                        "Uygulama otomatik olarak FTDI, CP210x, CH340 ve PL2303 " +
                        "seri dönüştürücüleri algılar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("FTDI", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("FT232R / FT231X", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("Silicon Labs", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("CP210x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("WCH", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("CH340/CH341", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("Prolific", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("PL2303", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
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
                            RadioButton(
                                selected = state.baudRate == rate,
                                onClick = {
                                    viewModel.updateBaudRate(rate)
                                    showBaudRateDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$rate bps", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBaudRateDialog = false }) { Text("İptal") } }
        )
    }
}