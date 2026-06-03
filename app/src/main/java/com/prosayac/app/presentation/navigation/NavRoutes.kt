package com.prosayac.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(
    val route: String,
    val title: String = "",
    val icon: ImageVector? = null
) {
    data object Dashboard : NavRoute("dashboard", "Gösterge Paneli", Icons.Default.Dashboard)
    data object Meters : NavRoute("meters", "Sayaçlar", Icons.Default.Speed)
    data object Readings : NavRoute("readings", "Okumalar", Icons.Default.ListAlt)
    data object Logs : NavRoute("logs", "Sistem Günlükleri", Icons.Default.Terminal)
    data object Settings : NavRoute("settings", "Ayarlar", Icons.Default.Settings)
    data object Connection : NavRoute("connection", "Bağlantı", Icons.Default.Usb)
}

val navItems = listOf(
    NavRoute.Dashboard,
    NavRoute.Meters,
    NavRoute.Readings,
    NavRoute.Logs,
    NavRoute.Connection,
    NavRoute.Settings
)
