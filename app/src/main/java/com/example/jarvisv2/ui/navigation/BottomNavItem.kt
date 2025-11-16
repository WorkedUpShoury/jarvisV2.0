package com.example.jarvisv2.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object System : BottomNavItem("system", Icons.Default.Settings, "System")
    object Apps : BottomNavItem("apps", Icons.Default.Apps, "Apps")
    object Web : BottomNavItem("web", Icons.Default.Language, "Web")
    object Chat : BottomNavItem("chat", Icons.AutoMirrored.Filled.Chat, "Chat")
}
