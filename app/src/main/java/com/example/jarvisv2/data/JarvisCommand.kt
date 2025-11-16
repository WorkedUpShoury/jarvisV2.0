package com.example.jarvisv2.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector

data class JarvisCommand(
    val command: String,
    val description: String,
    val icon: ImageVector? = null
)

// I've parsed the rules from your run-jarvis.pyw file
val quickCommands = listOf(
    JarvisCommand("lock screen", "Lock PC", Icons.Default.Lock),
    JarvisCommand("shutdown", "Shutdown PC", Icons.Default.PowerSettingsNew),
    JarvisCommand("restart", "Restart PC", Icons.Default.RestartAlt),
    JarvisCommand("sleep system", "Sleep PC", Icons.Default.Computer), // Using Computer as placeholder
    JarvisCommand("close", "Close Window", Icons.Default.Close),
    JarvisCommand("fullscreen", "Fullscreen", Icons.Default.Fullscreen),
)

val allCommands = listOf(
    JarvisCommand("get news", "Get latest news", Icons.Default.Search),
    JarvisCommand("get weather", "Get weather", Icons.Default.Search),
    JarvisCommand("remember last conversation", "Save conversation", Icons.Default.Settings),
    JarvisCommand("tell me ", "Ask Jarvis a question", Icons.Default.Search),
    JarvisCommand("handsfree mode on", "Turn on TWS mode", Icons.Default.MusicNote),
    JarvisCommand("handsfree mode off", "Turn off TWS mode", Icons.Default.MusicNote),
    JarvisCommand("enable gestures", "Enable gestures", Icons.Default.Settings),
    JarvisCommand("disable gestures", "Disable gestures", Icons.Default.Settings),
    JarvisCommand("open spotify", "Open Spotify", Icons.Default.MusicNote),
    JarvisCommand("spotify play liked songs", "Play Spotify liked songs", Icons.Default.MusicNote),
    JarvisCommand("play music", "Play music (liked songs)", Icons.Default.MusicNote),
    JarvisCommand("spotify ", "Play on Spotify", Icons.Default.MusicNote),
    JarvisCommand("watch ", "Watch a movie", Icons.Default.Settings),
    JarvisCommand("chrome", "Open Chrome", Icons.Default.Search),
    JarvisCommand("notepad", "Open Notepad", Icons.Default.Settings),
    JarvisCommand("command prompt", "Open Terminal", Icons.Default.Settings),
    JarvisCommand("file explorer", "Open File Explorer", Icons.Default.Settings),
    JarvisCommand("open settings", "Open PC Settings", Icons.Default.Settings),
    JarvisCommand("task manager", "Open Task Manager", Icons.Default.Settings),
    JarvisCommand("snipping tool", "Open Snipping Tool", Icons.Default.Settings),
    JarvisCommand("photoshop", "Open Photoshop", Icons.Default.Settings),
    JarvisCommand("brave", "Open Brave", Icons.Default.Search),
    JarvisCommand("obsidian", "Open Obsidian", Icons.Default.Settings),
    JarvisCommand("after effects", "Open After Effects", Icons.Default.Settings),
    JarvisCommand("telegram", "Open Telegram", Icons.Default.Send),
    JarvisCommand("whatsapp", "Open WhatsApp", Icons.Default.Send),
    JarvisCommand("call ", "Call on WhatsApp", Icons.Default.Send),
    JarvisCommand("send message to ", "Send WhatsApp message", Icons.Default.Send),
    JarvisCommand("end call", "End WhatsApp call", Icons.Default.Close),
    JarvisCommand("calculator", "Open Calculator", Icons.Default.Settings),
    JarvisCommand("valorant", "Open Valorant", Icons.Default.Settings),
    JarvisCommand("vscode", "Open VS Code", Icons.Default.Settings),
    JarvisCommand("resume playback", "Resume media", Icons.Default.MusicNote),
    JarvisCommand("pause playback", "Pause media", Icons.Default.MusicNote),
    JarvisCommand("next track", "Next track", Icons.Default.MusicNote),
    JarvisCommand("previous track", "Previous track", Icons.Default.MusicNote),
    JarvisCommand("status", "Run diagnostics", Icons.Default.Computer),
    JarvisCommand("set brightness to ", "Set brightness (0-10)", Icons.Default.BrightnessMedium),
    JarvisCommand("set volume to ", "Set volume (0-10)", Icons.Default.VolumeUp),
    JarvisCommand("high brightness", "Max brightness", Icons.Default.BrightnessMedium),
    JarvisCommand("low brightness", "Min brightness", Icons.Default.BrightnessMedium),
    JarvisCommand("high volume", "Max volume", Icons.Default.VolumeUp),
    JarvisCommand("low volume", "Min volume", Icons.Default.VolumeUp),
    JarvisCommand("mute volume", "Mute volume", Icons.Default.VolumeUp),
    JarvisCommand("turn on wifi", "Turn on WiFi", Icons.Default.Settings),
    JarvisCommand("turn off wifi", "Turn off WiFi", Icons.Default.Settings),
    JarvisCommand("turn on bluetooth", "Turn on Bluetooth", Icons.Default.Settings),
    JarvisCommand("turn off bluetooth", "Turn off Bluetooth", Icons.Default.Settings),
    JarvisCommand("airplane mode", "Toggle airplane mode", Icons.Default.Settings),
    JarvisCommand("alt tab", "Switch app", Icons.Default.Computer),
    JarvisCommand("close", "Close window", Icons.Default.Close),
    JarvisCommand("minimize", "Minimize window", Icons.Default.Computer),
    JarvisCommand("maximize", "Maximize window", Icons.Default.Computer),
    JarvisCommand("fullscreen", "Toggle fullscreen", Icons.Default.Fullscreen),
    JarvisCommand("what is the time", "Ask for time", Icons.Default.Search),
    JarvisCommand("what is the date", "Ask for date", Icons.Default.Search),
    JarvisCommand("next task", "Ask for next task", Icons.Default.Search),
    JarvisCommand("press enter", "Press Enter key", Icons.Default.Send),
    JarvisCommand("press ", "Press a key", Icons.Default.Send),
    JarvisCommand("send ", "Type and send", Icons.Default.Send),
    JarvisCommand("type ", "Type text", Icons.Default.Send),
    JarvisCommand("disable password", "Disable session password", Icons.Default.Shield),
    JarvisCommand("enable password", "Enable session password", Icons.Default.Shield),
    JarvisCommand("lock screen", "Lock PC", Icons.Default.Lock),
    JarvisCommand("shutdown", "Shutdown PC", Icons.Default.PowerSettingsNew),
    JarvisCommand("restart", "Restart PC", Icons.Default.RestartAlt),
    JarvisCommand("sleep system", "Sleep PC", Icons.Default.Computer),
    JarvisCommand("go to sleep", "Put Jarvis to sleep", Icons.Default.Computer),
    JarvisCommand("refresh", "Restart Jarvis server", Icons.Default.RestartAlt),
    JarvisCommand("search ", "Search web", Icons.Default.Search),
    JarvisCommand("search on youtube", "Search YouTube", Icons.Default.Search),
    JarvisCommand("play ", "Play on YouTube", Icons.Default.MusicNote),
    JarvisCommand("routine", "Get today's routine", Icons.Default.Search),
    JarvisCommand("screen", "Get screen info", Icons.Default.Computer),
    JarvisCommand("remind me to ", "Set a reminder", Icons.Default.Settings),
)