package com.prosayac.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// PRO MAX COLOR SYSTEM — Industrial Grey + Safety Orange
// =============================================================================
// Primary: Slate greys for data-dense dashboards
// CTA: Safety Orange (#F97316) for critical actions
// Background: Near-white slate (#F8FAFC) for maximum contrast

// Light Colors
val ProMaxPrimary = Color(0xFF475569)         // Slate-600
val ProMaxOnPrimary = Color(0xFFFFFFFF)
val ProMaxPrimaryContainer = Color(0xFFE2E8F0) // Slate-200
val ProMaxOnPrimaryContainer = Color(0xFF1E293B) // Slate-800

val ProMaxSecondary = Color(0xFF64748B)        // Slate-500
val ProMaxOnSecondary = Color(0xFFFFFFFF)
val ProMaxSecondaryContainer = Color(0xFFF1F5F9) // Slate-100
val ProMaxOnSecondaryContainer = Color(0xFF334155) // Slate-700

val ProMaxTertiary = Color(0xFFF97316)         // Safety Orange
val ProMaxOnTertiary = Color(0xFFFFFFFF)
val ProMaxTertiaryContainer = Color(0xFFFFEDD5) // Orange-100
val ProMaxOnTertiaryContainer = Color(0xFF9A3412) // Orange-800

val ProMaxError = Color(0xFFDC2626)             // Red-600
val ProMaxOnError = Color(0xFFFFFFFF)
val ProMaxErrorContainer = Color(0xFFFEE2E2)    // Red-100
val ProMaxOnErrorContainer = Color(0xFF991B1B)  // Red-800

val ProMaxBackground = Color(0xFFF8FAFC)        // Slate-50
val ProMaxOnBackground = Color(0xFF0F172A)      // Slate-900
val ProMaxSurface = Color(0xFFFFFFFF)
val ProMaxOnSurface = Color(0xFF0F172A)
val ProMaxSurfaceVariant = Color(0xFFF1F5F9)    // Slate-100
val ProMaxOnSurfaceVariant = Color(0xFF475569)  // Slate-600
val ProMaxOutline = Color(0xFFCBD5E1)           // Slate-300
val ProMaxOutlineVariant = Color(0xFFE2E8F0)    // Slate-200

// Dark Colors
val ProMaxDarkPrimary = Color(0xFF94A3B8)       // Slate-400
val ProMaxDarkOnPrimary = Color(0xFF0F172A)     // Slate-900
val ProMaxDarkPrimaryContainer = Color(0xFF334155) // Slate-700
val ProMaxDarkOnPrimaryContainer = Color(0xFFE2E8F0)

val ProMaxDarkSecondary = Color(0xFF64748B)     // Slate-500
val ProMaxDarkOnSecondary = Color(0xFF0F172A)
val ProMaxDarkSecondaryContainer = Color(0xFF1E293B)
val ProMaxDarkOnSecondaryContainer = Color(0xFFF1F5F9)

val ProMaxDarkTertiary = Color(0xFFF97316)      // Safety Orange (kept bright)
val ProMaxDarkOnTertiary = Color(0xFF0F172A)
val ProMaxDarkTertiaryContainer = Color(0xFF9A3412)
val ProMaxDarkOnTertiaryContainer = Color(0xFFFFEDD5)

val ProMaxDarkError = Color(0xFFFCA5A5)         // Red-300
val ProMaxDarkOnError = Color(0xFF7F1D1D)
val ProMaxDarkErrorContainer = Color(0xFF991B1B)
val ProMaxDarkOnErrorContainer = Color(0xFFFEE2E2)

val ProMaxDarkBackground = Color(0xFF0F172A)    // Slate-900
val ProMaxDarkOnBackground = Color(0xFFF8FAFC)
val ProMaxDarkSurface = Color(0xFF1E293B)       // Slate-800
val ProMaxDarkOnSurface = Color(0xFFF8FAFC)
val ProMaxDarkSurfaceVariant = Color(0xFF334155)
val ProMaxDarkOnSurfaceVariant = Color(0xFF94A3B8)
val ProMaxDarkOutline = Color(0xFF475569)
val ProMaxDarkOutlineVariant = Color(0xFF334155)

// Semantic Colors
val SyncSynced = Color(0xFF16A34A)              // Green-600
val SyncPending = Color(0xFFF97316)             // Orange-500
val SyncError = Color(0xFFDC2626)               // Red-600
val ChartBlue = Color(0xFF3B82F6)              // Blue-500
val ChartGreen = Color(0xFF22C55E)             // Green-500
val ChartOrange = Color(0xFFF97316)            // Orange-500
val ChartPurple = Color(0xFF8B5CF6)            // Violet-500

// =============================================================================
// PRO MAX TYPOGRAPHY — Fira Sans + Fira Code
// =============================================================================
val ProMaxFontFamily = FontFamily.Default
val ProMaxMonoFontFamily = FontFamily.Monospace

val ProMaxTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ProMaxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// =============================================================================
// LIGHT COLOR SCHEME
// =============================================================================
val ProMaxLightColorScheme = lightColorScheme(
    primary = ProMaxPrimary,
    onPrimary = ProMaxOnPrimary,
    primaryContainer = ProMaxPrimaryContainer,
    onPrimaryContainer = ProMaxOnPrimaryContainer,
    secondary = ProMaxSecondary,
    onSecondary = ProMaxOnSecondary,
    secondaryContainer = ProMaxSecondaryContainer,
    onSecondaryContainer = ProMaxOnSecondaryContainer,
    tertiary = ProMaxTertiary,
    onTertiary = ProMaxOnTertiary,
    tertiaryContainer = ProMaxTertiaryContainer,
    onTertiaryContainer = ProMaxOnTertiaryContainer,
    error = ProMaxError,
    onError = ProMaxOnError,
    errorContainer = ProMaxErrorContainer,
    onErrorContainer = ProMaxOnErrorContainer,
    background = ProMaxBackground,
    onBackground = ProMaxOnBackground,
    surface = ProMaxSurface,
    onSurface = ProMaxOnSurface,
    surfaceVariant = ProMaxSurfaceVariant,
    onSurfaceVariant = ProMaxOnSurfaceVariant,
    outline = ProMaxOutline,
    outlineVariant = ProMaxOutlineVariant
)

// =============================================================================
// DARK COLOR SCHEME
// =============================================================================
val ProMaxDarkColorScheme = darkColorScheme(
    primary = ProMaxDarkPrimary,
    onPrimary = ProMaxDarkOnPrimary,
    primaryContainer = ProMaxDarkPrimaryContainer,
    onPrimaryContainer = ProMaxDarkOnPrimaryContainer,
    secondary = ProMaxDarkSecondary,
    onSecondary = ProMaxDarkOnSecondary,
    secondaryContainer = ProMaxDarkSecondaryContainer,
    onSecondaryContainer = ProMaxDarkOnSecondaryContainer,
    tertiary = ProMaxDarkTertiary,
    onTertiary = ProMaxDarkOnTertiary,
    tertiaryContainer = ProMaxDarkTertiaryContainer,
    onTertiaryContainer = ProMaxDarkOnTertiaryContainer,
    error = ProMaxDarkError,
    onError = ProMaxDarkOnError,
    errorContainer = ProMaxDarkErrorContainer,
    onErrorContainer = ProMaxDarkOnErrorContainer,
    background = ProMaxDarkBackground,
    onBackground = ProMaxDarkOnBackground,
    surface = ProMaxDarkSurface,
    onSurface = ProMaxDarkOnSurface,
    surfaceVariant = ProMaxDarkSurfaceVariant,
    onSurfaceVariant = ProMaxDarkOnSurfaceVariant,
    outline = ProMaxDarkOutline,
    outlineVariant = ProMaxDarkOutlineVariant
)

// =============================================================================
// PRO MAX SHAPES — Slightly rounded for industrial feel
// =============================================================================
val ProMaxShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// =============================================================================
// PRO MAX THEME WRAPPER
// =============================================================================
@Composable
fun ProMaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ProMaxDarkColorScheme else ProMaxLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ProMaxTypography,
        shapes = ProMaxShapes,
        content = content
    )
}