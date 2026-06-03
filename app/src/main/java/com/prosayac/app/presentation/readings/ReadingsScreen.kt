package com.prosayac.app.presentation.readings

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.prosayac.app.data.local.dao.ReadingWithMeter
import com.prosayac.app.presentation.components.SyncStatusBadge
import com.prosayac.app.presentation.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingsScreen(
    viewModel: ReadingsViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Okumalar", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menü") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (!state.isLoading && state.readings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(
                        onClick = { viewModel.exportToCsv() },
                        containerColor = ProMaxTertiary,
                        contentColor = ProMaxOnTertiary
                    ) {
                        Icon(Icons.Default.FileDownload, "CSV Dışa Aktar")
                    }
                    FloatingActionButton(
                        onClick = { /* Sync */ },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Sync, "Senkronize Et")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(state.isExporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ProMaxTertiary)
            }

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

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ProMaxTertiary)
                }
            } else if (state.readings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ListAlt, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Henüz okuma kaydı bulunmamaktadır")
                    }
                }
            } else {
                val pageSize = state.pageSize
                val totalPages = (state.readings.size + pageSize - 1) / pageSize
                val currentPage = state.currentPage.coerceIn(1, totalPages.coerceAtLeast(1))
                val startIndex = (currentPage - 1) * pageSize
                val endIndex = (startIndex + pageSize).coerceAtMost(state.readings.size)
                val pageReadings = state.readings.subList(startIndex, endIndex)

                val scrollState = rememberScrollState()
                val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

                Column(modifier = Modifier.weight(1f)) {
                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        TableHeaderCell("Seri No", 130.dp)
                        TableHeaderCell("Daire", 60.dp)
                        TableHeaderCell("Bina", 80.dp)
                        TableHeaderCell("Okuma Değeri", 110.dp)
                        TableHeaderCell("Tarih", 90.dp)
                        TableHeaderCell("Durum", 70.dp)
                        TableHeaderCell("Senkron", 80.dp)
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(pageReadings) { reading ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell(reading.serialNumber.ifBlank { "-" }, 130.dp, mono = true)
                                TableCell(reading.flatNumber.ifBlank { "-" }, 60.dp)
                                TableCell(reading.buildingName.ifBlank { "-" }, 80.dp, mono = true)
                                // READING VALUE - the actual numeric reading (e.g., "150 kWh")
                                TableCell(
                                    text = reading.readingValue.ifBlank { "-" },
                                    width = 110.dp,
                                    mono = true
                                )
                                TableCell(
                                    text = dateFormat.format(Date(reading.readingDate)),
                                    width = 90.dp
                                )
                                // Status badge
                                Surface(
                                    modifier = Modifier.width(70.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = when (reading.meterStatus) {
                                        "Read" -> SyncSynced.copy(alpha = 0.12f)
                                        "Skipped" -> ChartOrange.copy(alpha = 0.12f)
                                        else -> ProMaxError.copy(alpha = 0.12f)
                                    }
                                ) {
                                    Text(
                                        when (reading.meterStatus) {
                                            "Read" -> "Okundu"
                                            "Unread" -> "Okunamadı"
                                            "Skipped" -> "Atlandı"
                                            else -> reading.meterStatus
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (reading.meterStatus) {
                                            "Read" -> SyncSynced
                                            "Skipped" -> ChartOrange
                                            else -> ProMaxError
                                        }
                                    )
                                }
                                SyncStatusBadge(isSynced = reading.isSynced, modifier = Modifier.width(80.dp))
                            }
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }

                    // Pagination
                    if (totalPages > 1) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.previousPage() }, enabled = currentPage > 1) {
                                Icon(Icons.Default.ChevronLeft, "Önceki")
                            }
                            Text("$currentPage / $totalPages", fontFamily = FontFamily.Monospace)
                            IconButton(onClick = { viewModel.nextPage() }, enabled = currentPage < totalPages) {
                                Icon(Icons.Default.ChevronRight, "Sonraki")
                            }
                        }
                        Text(
                            "${state.readings.size} kayıt (${startIndex + 1}-${endIndex} arası)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Export dialog
    if (state.showExportDone && state.exportPath != null) {
        val exportPath = state.exportPath!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDone() },
            title = { Text("Dışa Aktarma Tamamlandı", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Okumalar başarıyla dışa aktarıldı:")
                    Text(exportPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissExportDone() }) { Text("Tamam") } },
            dismissButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(exportPath))
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Paylaş"))
                }) { Text("Paylaş") }
            }
        )
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, mono: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}