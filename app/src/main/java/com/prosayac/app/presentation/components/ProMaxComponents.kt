package com.prosayac.app.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prosayac.app.presentation.theme.*

/**
 * Premium KPI Stat Card with subtle gradient overlay.
 */
@Composable
fun KpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null,
    progress: Float? = null
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                icon?.invoke()
            }
            progress?.let { p ->
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { p.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.12f)
                )
            }
        }
    }
}

/**
 * SyncStatusBadge — Green/Orange indicator for sync state.
 */
@Composable
fun SyncStatusBadge(
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isSynced) SyncSynced else SyncPending
    val text = if (isSynced) "Senkron" else "Bekliyor"

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * GlowingStatusIndicator — Animated pulsing dot for connection state.
 */
@Composable
fun GlowingStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    val color = if (isConnected) SyncSynced else SyncError
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = if (isConnected) alpha else 1f))
    )
}

/**
 * DangerZoneCard — Red-bordered destructive action container.
 */
@Composable
fun DangerZoneCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAction,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    )
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}