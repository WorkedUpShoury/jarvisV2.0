package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.viewmodel.MainViewModel

/**
 * List of commonly used apps & tools.
 * These buttons trigger remote commands.
 */
private val appActions = listOf(
    Action("Spotify", Icons.Default.MusicNote, "open spotify"),
    Action("Chrome", Icons.Default.TravelExplore, "open chrome"),
    Action("Brave", Icons.Default.Shield, "open brave"),
    Action("VS Code", Icons.Default.Code, "open vscode"),
    Action("Notepad", Icons.Default.Notes, "open notepad"),
    Action("Terminal", Icons.Default.Terminal, "open terminal"),
    Action("Explorer", Icons.Default.Folder, "file explorer"),
    Action("Task Mgr", Icons.Default.Memory, "task manager"),
    Action("Valorant", Icons.Default.Games, "open valorant"),
    Action("YouTube", Icons.Default.VideoLibrary, "open youtube"),
    Action("WhatsApp", Icons.Default.Send, "open whatsapp"),
    Action("Calculator", Icons.Default.Calculate, "open calculator"),
)

@Composable
fun AppsScreen(viewModel: MainViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(appActions) { action ->
            ActionButton(
                icon = action.icon,
                text = action.name,
                onClick = { viewModel.sendCommand(action.command) }
            )
        }
    }
}
