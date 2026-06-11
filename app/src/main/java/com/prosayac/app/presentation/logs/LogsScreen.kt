package com.prosayac.app.presentation.logs

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.prosayac.app.util.log.LogEntry
import com.prosayac.app.util.log.LogTag
import com.prosayac.app.presentation.theme.*
import java.io.File

// Terminal color palette
private val TerminalBg = Color(0xFF0C0C0C)
private val TerminalSurface = Color(0xFF161616)
private val TerminalBorder = Color(0xFF2A2A2A)
private val TerminalText = Color(0xFFE0E0E0)
private val TerminalInfoWhite = Color(0xFFFFFFFF)
private val TerminalWarnYellow = Color(0xFFFFB300)
private val TerminalErrorRed = Color(0xFFFF5252)
private val TerminalHardwareCyan = Color(0xFF00E5FF)
private val TerminalParserViolet = Color(0xFFB388FF)
private val TerminalTimestamp = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Auto-scroll to latest entry
    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sistem Günlükleri",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, "Menü")
                    }
                },
                actions = {
                    // Clear logs button
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, "Günlükleri Temizle")
                    }
                    // Share/export button
                    IconButton(onClick = { viewModel.exportLogs() }) {
                        Icon(Icons.Default.Share, "Günlükleri Paylaş")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalSurface,
                    titleContentColor = TerminalText
                )
            )
        },
        containerColor = TerminalBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TerminalBg)
        ) {
            // Terminal header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "KARA KUTU · TERMİNAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalTimestamp,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${state.logs.size} kayıt", style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace, color = TerminalTimestamp)
                    // Live indicator dot
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = RoundedCornerShape(50),
                        color = TerminalHardwareCyan
                    ) {}
                }
            }

            HorizontalDivider(color = TerminalBorder, thickness = 0.5.dp)

            // Log entries list
            if (state.logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Terminal,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = TerminalTimestamp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Henüz günlük kaydı bulunmamaktadır",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TerminalTimestamp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Donanım bağlantısı kurulduğunda veya sayaç okuma başlatıldığında\ngünlükler burada görüntülenecektir",
                            style = MaterialTheme.typography.bodySmall,
                            color = TerminalTimestamp.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(state.logs, key = { it.timestamp.hashCode() + it.message.hashCode() }) { entry ->
                        LogEntryRow(entry)
                    }
                    // Bottom spacer for FAB clearance
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }

            // Bottom action bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TerminalSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clear button
                    OutlinedButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TerminalErrorRed
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder)
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Günlükleri Temizle", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    // Export/share button
                    Button(
                        onClick = { viewModel.exportLogs() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalHardwareCyan.copy(alpha = 0.2f),
                            contentColor = TerminalHardwareCyan
                        )
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Günlükleri Paylaş", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Export done dialog
    if (state.showExportDone && state.exportPath != null) {
        val exportPath = state.exportPath!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDone() },
            containerColor = TerminalSurface,
            titleContentColor = TerminalText,
            textContentColor = TerminalTimestamp,
            title = { Text("Dışa Aktarıldı", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    Text("Sistem günlükleri başarıyla dışa aktarıldı:", fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        exportPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = TerminalHardwareCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissExportDone() }) {
                    Text("Tamam", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.shareExport()
                    viewModel.dismissExportDone()
                }) {
                    Text("Paylaş", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

// =============================================================================
// LOG ENTRY ROW (TERMINAL STYLE)
// =============================================================================
@Composable
fun LogEntryRow(entry: LogEntry) {
    val tagColor = when (entry.tag) {
        LogTag.INFO -> TerminalInfoWhite
        LogTag.WARN -> TerminalWarnYellow
        LogTag.ERROR -> TerminalErrorRed
        LogTag.HARDWARE -> TerminalHardwareCyan
        LogTag.PARSER -> TerminalParserViolet
        LogTag.SYNC -> TerminalInfoWhite
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp column
        Text(
            text = entry.formattedTime,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TerminalTimestamp,
            modifier = Modifier.width(60.dp)
        )

        // Tag badge
        Surface(
            modifier = Modifier.padding(end = 6.dp),
            shape = RoundedCornerShape(3.dp),
            color = tagColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = entry.tag.displayName,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = tagColor,
                letterSpacing = 0.5.sp
            )
        }

        // Message
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TerminalText,
            modifier = Modifier.weight(1f),
            lineHeight = 14.sp
        )
    }
}