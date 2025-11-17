package com.example.jarvisv2.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object System : BottomNavItem("system", Icons.Default.Settings, "System")
    object Media : BottomNavItem("media", Icons.Default.MusicNote, "Media") // <-- This is the missing line
    object Apps : BottomNavItem("apps", Icons.Default.Apps, "Apps")
    object Web : BottomNavItem("web", Icons.Default.Language, "Web")
    object Chat : BottomNavItem("chat", Icons.AutoMirrored.Filled.Chat, "Chat")
}