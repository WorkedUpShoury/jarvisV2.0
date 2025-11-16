package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.viewmodel.MainViewModel

/**
 * List of system actions shown in the System tab.
 */
private val systemActions = listOf(
    Action("Lock", Icons.Default.Lock, "lock screen"),
    Action("Shutdown", Icons.Default.PowerSettingsNew, "shutdown"),
    Action("Restart", Icons.Default.RestartAlt, "restart"),
    Action("Sleep", Icons.Default.Bedtime, "sleep system"),
    Action("Vol Up", Icons.Default.VolumeUp, "set volume to 8"),
    Action("Vol Down", Icons.Default.VolumeDown, "set volume to 2"),
    Action("Mute", Icons.Default.VolumeOff, "mute volume"),
    Action("Bright Up", Icons.Default.BrightnessHigh, "increase brightness"),
    Action("Bright Low", Icons.Default.BrightnessLow, "decrease brightness"),
    Action("Close", Icons.Default.Close, "close window"),
    Action("Fullscreen", Icons.Default.Fullscreen, "fullscreen"),
    Action("Minimize", Icons.Default.Minimize, "minimize window"),
)

@Composable
fun SystemScreen(viewModel: MainViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(systemActions) { action ->
            ActionButton(
                icon = action.icon,
                text = action.name,
                onClick = { viewModel.sendCommand(action.command) }
            )
        }
    }
}
