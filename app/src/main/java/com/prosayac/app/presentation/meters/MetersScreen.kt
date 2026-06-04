package com.prosayac.app.presentation.meters

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prosayac.app.domain.model.Meter
import com.prosayac.app.presentation.components.SyncStatusBadge
import com.prosayac.app.presentation.theme.*
import com.prosayac.app.util.excel.ExcelFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetersScreen(
    viewModel: MetersViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showTypeFilter by remember { mutableStateOf(false) }
    var showStatusFilter by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Sayaçlar", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menü") }
                },
                actions = {
                    // IMPORT/EXCEL button
                    IconButton(onClick = { viewModel.toggleDropzone() }) {
                        Icon(Icons.Default.FileOpen, "İçe Aktar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            // "Okumaya Başla" FAB - triggers real M-Bus hardware polling
            if (state.meters.isNotEmpty() && !state.isReadingInProgress) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.startReading() },
                    containerColor = ProMaxTertiary,
                    contentColor = ProMaxOnTertiary
                ) {
                    Icon(Icons.Default.Usb, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Okumaya Başla", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            // Cancel button when reading is in progress
            if (state.isReadingInProgress) {
                FloatingActionButton(
                    onClick = { viewModel.cancelReading() },
                    containerColor = ProMaxError
                ) {
                    Icon(Icons.Default.Close, "Okumayı İptal Et")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Import progress
            AnimatedVisibility(state.isImporting) {
                Column {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ProMaxTertiary)
                    if (state.importProgress.isNotEmpty()) {
                        Text(
                            state.importProgress,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = ProMaxTertiary
                        )
                    }
                }
            }

            // Reading in progress banner
            AnimatedVisibility(state.isReadingInProgress) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = ChartBlue.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ChartBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            state.readingProgressMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ChartBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Dropzone
            AnimatedVisibility(
                visible = state.showDropzone,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ExcelDropzone(
                    onFilePick = { filePickerLauncher.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel"
                    )) },
                    onDismiss = { viewModel.toggleDropzone() }
                )
            }

            // Filters bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    FilterChip(
                        selected = state.selectedTypeFilter != "All",
                        onClick = { showTypeFilter = true },
                        label = {
                            Text(when (state.selectedTypeFilter) {
                                "Isı Sayacı" -> "Isı Sayacı"
                                "Sıcak Su Sayacı" -> "Sıcak Su Sayacı"
                                else -> "Tüm Tipler"
                            }, style = MaterialTheme.typography.labelSmall)
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(expanded = showTypeFilter, onDismissRequest = { showTypeFilter = false }) {
                        listOf("All" to "Tüm Tipler", "Isı Sayacı" to "Isı Sayacı", "Sıcak Su Sayacı" to "Sıcak Su Sayacı").forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setTypeFilter(value); showTypeFilter = false })
                        }
                    }
                }
                Box {
                    FilterChip(
                        selected = state.selectedStatusFilter != "All",
                        onClick = { showStatusFilter = true },
                        label = {
                            Text(when (state.selectedStatusFilter) {
                                "Read" -> "Okundu"
                                "Unread" -> "Okunamadı"
                                "Skipped" -> "Atlandı"
                                else -> "Tüm Durumlar"
                            }, style = MaterialTheme.typography.labelSmall)
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(expanded = showStatusFilter, onDismissRequest = { showStatusFilter = false }) {
                        listOf("All" to "Tüm Durumlar", "Read" to "Okundu", "Unread" to "Okunamadı", "Skipped" to "Atlandı").forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setStatusFilter(value); showStatusFilter = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("${state.filteredMeters.size} / ${state.meters.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace)
            }

            // Error
            state.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) { Icon(Icons.Default.Close, "Kapat") }
                    }
                }
            }

            // Content
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ProMaxTertiary)
                }
            } else if (state.filteredMeters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Henüz sayaç bulunmamaktadır", style = MaterialTheme.typography.bodyLarge)
                        Text("Excel dosyası içe aktararak başlayabilirsiniz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredMeters, key = { it.id }) { meter ->
                        val readStatus = state.meterReadStatuses[meter.id]
                        val readingValue = state.meterReadingValues[meter.id]
                        MeterGridCard(
                            meter = meter,
                            readStatus = readStatus,
                            readingValue = readingValue,
                            isReadingInProgress = state.isReadingInProgress
                        )
                    }
                }
            }
        }
    }

    // Building Name dialog - shown BEFORE import
    if (state.showBuildingNameDialog) {
        BuildingNameDialog(
            onConfirm = { name -> viewModel.confirmBuildingName(name) },
            onDismiss = { viewModel.dismissBuildingNameDialog() }
        )
    }

    // Ambiguous format dialog
    if (state.pendingFormatChoiceUri != null && state.pendingFormatOptions.isNotEmpty()) {
        FormatSelectionDialog(
            options = state.pendingFormatOptions,
            onSelected = { viewModel.onFormatPicked(it) },
            onDismiss = { viewModel.dismissFormatChoice() }
        )
    }

    // Import result dialog
    if (state.showImportResult && state.importResult != null) {
        val result = state.importResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportResult() },
            title = { Text("İçe Aktarma Sonucu", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("${result.successCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SyncSynced)
                            Text("Başarılı", style = MaterialTheme.typography.labelSmall)
                        }
                        Column {
                            Text("${result.errorCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ProMaxError)
                            Text("Hatalı", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (result.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        result.errors.take(5).forEach { err ->
                            Text("Satır ${err.row}: ${err.message}", style = MaterialTheme.typography.bodySmall, color = ProMaxError)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissImportResult() }) { Text("Tamam") } }
        )
    }
}

// =============================================================================
// BUILDING NAME DIALOG
// =============================================================================
@Composable
fun BuildingNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var buildingName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Business, null, tint = ProMaxTertiary)
        },
        title = {
            Text("Bina İsmi", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "İçe aktarılan sayaçların hangi binaya ait olduğunu giriniz:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = buildingName,
                    onValueChange = { buildingName = it },
                    label = { Text("Bina İsmi") },
                    placeholder = { Text("Örn: A Blok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (buildingName.isNotBlank()) {
                                onConfirm(buildingName.trim())
                            }
                        }
                    ),
                    leadingIcon = { Icon(Icons.Default.Business, null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (buildingName.isNotBlank()) {
                        onConfirm(buildingName.trim())
                    }
                },
                enabled = buildingName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ProMaxTertiary)
            ) {
                Text("İçe Aktar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

// =============================================================================
// FORMAT SELECTION DIALOG
// =============================================================================
@Composable
fun FormatSelectionDialog(
    options: List<ExcelFormat>,
    onSelected: (ExcelFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hangi formatı kullanıyorsun?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Başlık satırı birden fazla formata uyuyor. Lütfen doğru formatı seçin:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { option ->
                    val label = when (option) {
                        ExcelFormat.TELEGRAM -> "Telegram"
                        ExcelFormat.POLIMETER -> "Polimeter"
                        ExcelFormat.STANDARD -> "Standart"
                    }
                    OutlinedButton(
                        onClick = { onSelected(option) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ProMaxTertiary)
                    ) {
                        Text(label, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

// =============================================================================
// GRID METER CARD (HARDWARE POLLING STATUS INTEGRATED, NO MANUAL BUTTONS)
// =============================================================================
@Composable
fun MeterGridCard(
    meter: Meter,
    readStatus: String? = null,
    readingValue: String? = null,
    isReadingInProgress: Boolean = false
) {
    val statusColor = when (meter.status) {
        "Read" -> SyncSynced
        "Skipped" -> ChartOrange
        else -> ProMaxError
    }

    // Override status display during active polling
    val displayStatus = when {
        isReadingInProgress && readStatus == "polling" -> "Okunuyor..."
        isReadingInProgress && readStatus == "success" -> "Okundu ✓"
        isReadingInProgress && readStatus == "timeout" -> "Cihaz Yanıt Vermedi"
        isReadingInProgress && readStatus == "error" -> "Hata"
        else -> meter.displayStatus
    }

    val displayStatusColor = when {
        isReadingInProgress && readStatus == "polling" -> ChartBlue
        isReadingInProgress && readStatus == "success" -> SyncSynced
        isReadingInProgress && readStatus == "timeout" -> ProMaxError
        isReadingInProgress && readStatus == "error" -> ProMaxError
        else -> statusColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Polling spinner indicator
            if (isReadingInProgress && readStatus == "polling") {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(0.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = ChartBlue
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Building name - prominent at top
            if (meter.buildingName.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = ProMaxTertiary.copy(alpha = 0.1f)
                ) {
                    Text(
                        meter.buildingName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = ProMaxTertiary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    meter.displaySerialNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(displayStatusColor)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("D: ${meter.displayFlatNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(meter.meterType, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))

            // Status badge
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = displayStatusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    displayStatus,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = displayStatusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Live reading value during polling
            if (isReadingInProgress && readingValue != null && readStatus == "success") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Endeks: $readingValue",
                    style = MaterialTheme.typography.bodySmall,
                    color = SyncSynced,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Last reading value (from DB / previous sessions)
            meter.lastReading?.let { reading ->
                if (!isReadingInProgress || readStatus != "success") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Son: $reading",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// =============================================================================
// EXCEL DROPZONE
// =============================================================================
@Composable
fun ExcelDropzone(
    onFilePick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    ProMaxTertiary,
                    ProMaxPrimary.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("EXCEL İÇE AKTAR", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Kapat")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(56.dp), tint = ProMaxTertiary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Dosyayı sürükleyin veya seçin", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Desteklenen formatlar:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("• Telegram (.xlsx / .xls)", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text("• Polimeter (.xlsx / .xls)", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text("• Genel sayaç listesi (.xlsx / .xls / .csv)", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onFilePick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ProMaxTertiary)
            ) {
                Icon(Icons.Default.FileOpen, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Excel Dosyası Seç")
            }
        }
    }
}