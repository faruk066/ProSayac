package com.prosayac.app.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prosayac.app.presentation.components.KpiCard
import com.prosayac.app.presentation.theme.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToMeters: () -> Unit,
    onMenuClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh dashboard data every time this screen becomes visible
    // (e.g., after returning from Meters screen post-reading session)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadAllData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gösterge Paneli",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ProMaxTertiary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // KPI SUMMARY CARDS (2x2 Grid)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "TOPLAM",
                            value = state.totalMeters.toString(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            accentColor = ChartBlue,
                            icon = { Icon(Icons.Default.Speed, null, tint = ChartBlue) }
                        )
                        KpiCard(
                            title = "OKUNAN",
                            value = state.readMeters.toString(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            accentColor = ChartGreen,
                            progress = state.readingProgress,
                            icon = { Icon(Icons.Default.CheckCircle, null, tint = ChartGreen) }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "OKUNMAYAN",
                            value = state.unreadMeters.toString(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            accentColor = ProMaxError,
                            icon = { Icon(Icons.Default.Cancel, null, tint = ProMaxError) }
                        )
                        KpiCard(
                            title = "SENKRONİZE",
                            value = (state.totalMeters - state.unsyncedMeters).toString(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            accentColor = SyncSynced,
                            progress = state.syncProgress,
                            subtitle = "${state.unsyncedMeters} bekleyen",
                            icon = { Icon(Icons.Default.Sync, null, tint = SyncSynced) }
                        )
                    }
                }

                // Chart error banner
                if (state.isChartError && state.chartErrorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ProMaxError.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    null,
                                    tint = ProMaxError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.chartErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ProMaxError,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ========== BAR CHART — Okuma İstatistiği ==========
                item {
                    ChartCard(title = "OKUMA İSTATİSTİĞİ (7 Gün)") {
                        BarChart(
                            labels = state.barChartLabels,
                            values = state.barChartValues,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }

                // ========== QUICK ACTION CARD (standalone, full width) ==========
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = ProMaxTertiaryContainer
                        ),
                        onClick = onNavigateToMeters
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PlaylistAddCheck,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = ProMaxTertiary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Sayaç Okumaya Başla",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ProMaxOnTertiaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${state.unreadMeters} okunmamış sayaç",
                                style = MaterialTheme.typography.bodySmall,
                                color = ProMaxOnTertiaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ========== LINE CHART — Okuma Trendi ==========
                item {
                    ChartCard(title = "OKUMA TRENDİ (Aylık)") {
                        LineChart(
                            labels = state.lineChartLabels,
                            values = state.lineChartValues,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                // Bottom spacer
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

// =============================================================================
// CHART CARD WRAPPER
// =============================================================================
@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// =============================================================================
// CUSTOM BAR CHART — Canvas-based
// =============================================================================
@Composable
fun BarChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val barColor = ChartBlue
    val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(values) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    // Evaluate theme color in @Composable scope before entering Canvas DrawScope
    val axisLabelColorArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    // Hoist Paint objects outside draw loop to prevent GC pressure (100+ allocs/sec)
    val valueLabelPaint = remember {
        android.graphics.Paint().apply {
            color = barColor.toArgb()
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val axisLabelPaint = remember {
        android.graphics.Paint().apply {
            color = axisLabelColorArgb
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barCount = values.size.coerceAtLeast(1)
        val totalSpacing = canvasWidth * 0.25f
        val spacingBetween = totalSpacing / (barCount + 1)
        val barWidth = (canvasWidth - totalSpacing) / barCount
        val topMargin = 20f
        val chartHeight = canvasHeight - topMargin - 30f

        val progress = animatedProgress.value

        values.forEachIndexed { index, value ->
            val barHeight = (value / maxValue * chartHeight * progress)
            val x = spacingBetween + index * (barWidth + spacingBetween)
            val y = topMargin + chartHeight - barHeight

            // Bar with rounded top
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Value label on top (reuse hoisted Paint)
            if (barHeight > 20f) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${value.toInt()}",
                    x + barWidth / 2,
                    y - 6f,
                    valueLabelPaint
                )
            }
        }

        // X-axis labels (reuse hoisted Paint)
        labels.forEachIndexed { index, label ->
            val x = spacingBetween + index * (barWidth + spacingBetween) + barWidth / 2
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                canvasHeight - 4f,
                axisLabelPaint
            )
        }
    }
}

// =============================================================================
// CUSTOM DONUT CHART — Canvas-based arc segments
// =============================================================================
@Composable
fun DonutChart(
    segments: List<DonutSegmentUi>,
    modifier: Modifier = Modifier
) {
    val animatedSweep = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        animatedSweep.snapTo(0f)
        animatedSweep.animateTo(360f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val canvasSize = minOf(size.width, size.height)
            val strokeWidth = canvasSize * 0.22f
            val radius = (canvasSize - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val totalValue = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)

            var startAngle = -90f
            val sweepProgress = animatedSweep.value

            segments.forEach { segment ->
                val sweepAngle = (segment.value / totalValue) * sweepProgress

                drawArc(
                    color = Color(segment.color),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                startAngle += sweepAngle
            }
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${segments.size}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Segment",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Legend
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        segments.forEach { segment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(segment.color))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    segment.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

// =============================================================================
// CUSTOM LINE CHART — Canvas-based cubic bezier
// =============================================================================
@Composable
fun LineChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    // Safety: guard against empty values list that can crash during state transitions
    if (values.isEmpty()) return

    val lineColor = ChartOrange
    val fillColor = ChartOrange.copy(alpha = 0.15f)
    val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(values) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    // Evaluate theme color in @Composable scope before entering Canvas DrawScope
    val lineChartAxisColorArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    // Hoist Paint to prevent allocation per frame during animation
    val lineChartAxisPaint = remember {
        android.graphics.Paint().apply {
            color = lineChartAxisColorArgb
            textSize = 20f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val topMargin = 16f
        val bottomMargin = 32f
        val chartHeight = canvasHeight - topMargin - bottomMargin
        val pointCount = values.size.coerceAtLeast(2)

        val progress = animatedProgress.value
        val visiblePoints = ((pointCount - 1) * progress).toInt() + 1
        val partialProgress = (pointCount - 1) * progress - (visiblePoints - 1)

        val points = mutableListOf<Offset>()
        val fillPath = Path()

        for (i in 0 until visiblePoints) {
            // Safety clamp: ensure index is within bounds after coercion
            val safeIndex = i.coerceAtMost(values.size - 1)
            val x = if (pointCount > 1) i.toFloat() / (pointCount - 1) * canvasWidth else canvasWidth / 2
            val y = topMargin + chartHeight - (values[safeIndex] / maxValue * chartHeight)
            points.add(Offset(x, y))
        }

        // Handle partial point for smooth animation
        if (visiblePoints < pointCount && partialProgress > 0f) {
            val prevX = if (pointCount > 1) (visiblePoints - 1).toFloat() / (pointCount - 1) * canvasWidth else canvasWidth / 2
            val nextX = if (pointCount > 1) visiblePoints.toFloat() / (pointCount - 1) * canvasWidth else canvasWidth / 2
            val safePrevIdx = (visiblePoints - 1).coerceAtMost(values.size - 1)
            val safeNextIdx = visiblePoints.coerceAtMost(values.size - 1)
            val prevY = topMargin + chartHeight - (values[safePrevIdx] / maxValue * chartHeight)
            val nextY = topMargin + chartHeight - (values[safeNextIdx] / maxValue * chartHeight)
            val interpX = prevX + (nextX - prevX) * partialProgress
            val interpY = prevY + (nextY - prevY) * partialProgress
            points.add(Offset(interpX, interpY))
        }

        if (points.size >= 2) {
            // Fill path
            fillPath.moveTo(points.first().x, topMargin + chartHeight)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2
                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, topMargin + chartHeight)
            fillPath.close()

            drawPath(fillPath, fillColor)

            // Line path
            val linePath = Path()
            linePath.moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2
                linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
            drawPath(linePath, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            // Dots
            points.forEach { point ->
                drawCircle(lineColor, 5.dp.toPx(), point)
                drawCircle(Color.White, 3.dp.toPx(), point)
            }
        }

        // X-axis labels (reuse hoisted Paint)
        labels.forEachIndexed { index, label ->
            if (index < labels.size) {
                val x = if (pointCount > 1) index.toFloat() / (pointCount - 1) * canvasWidth else canvasWidth / 2
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    canvasHeight - 4f,
                    lineChartAxisPaint
                )
            }
        }
    }
}