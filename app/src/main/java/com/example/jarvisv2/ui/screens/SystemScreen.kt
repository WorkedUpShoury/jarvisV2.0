package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel
import kotlin.math.roundToInt

// 1. Maintenance Actions
private val maintenanceActions = listOf(
    Action("Refresh", Icons.Default.Refresh, "refresh"),
    Action("Sleep", Icons.Default.BedtimeOff, "go to sleep"), // <--- Renamed to "Sleep" for better alignment
    Action("Gestures", Icons.Default.BackHand, "enable gestures"),
    Action("Diagnostics", Icons.Default.MonitorHeart, "status")
)

// 2. Window Actions
private val windowRowActions = listOf(
    Action("Minimize", Icons.Default.Minimize, "minimize window"),
    Action("Fullscreen", Icons.Default.Fullscreen, "fullscreen"),
    Action("Close", Icons.Default.Close, "close window")
)

// 3. System Power Actions
private val systemGridActions = listOf(
    Action("Lock", Icons.Default.Lock, "lock screen"),
    Action("Shutdown", Icons.Default.PowerSettingsNew, "shutdown"),
    Action("Restart", Icons.Default.RestartAlt, "restart"),
    Action("Sleep PC", Icons.Default.Bedtime, "sleep system"),
)

@Composable
fun SystemScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- Maintenance Section (Top) ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), // 4 items in one row
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(100.dp) // Fixed height for single row
        ) {
            items(maintenanceActions) { action ->
                ActionButton(
                    modifier = Modifier.fillMaxHeight(),
                    icon = action.icon,
                    text = action.name,
                    viewModel = viewModel,
                    command = action.command,
                    // --- TRANSPARENCY RESTORED ---
                    containerColor = Color.Transparent,
                    elevation = 0.dp
                )
            }
        }

        // --- ADDED SPACING ---
        Spacer(modifier = Modifier.height(24.dp))

        // --- Window Actions ---
        WindowActionsCard(viewModel = viewModel)

        // --- Sliders & Power Actions ---
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                VolumeSlider(viewModel)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrightnessSlider(viewModel)
            }

            items(systemGridActions) { action ->
                ActionButton(
                    icon = action.icon,
                    text = action.name,
                    viewModel = viewModel,
                    command = action.command
                )
            }
        }
    }
}

@Composable
private fun WindowActionsCard(viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            windowRowActions.forEach { action ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.sendButtonCommand(action.command) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.name,
                        tint = DarkPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = action.name,
                        color = DarkOnSurface,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeSlider(viewModel: MainViewModel) {
    var sliderPosition by remember { mutableFloatStateOf(5f) }
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text("Volume: ${sliderPosition.roundToInt()}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..10f,
            steps = 9,
            onValueChangeFinished = {
                val command = "set volume to ${sliderPosition.roundToInt()}"
                viewModel.sendButtonCommand(command)
            },
            thumb = {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volume",
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessSlider(viewModel: MainViewModel) {
    var sliderPosition by remember { mutableFloatStateOf(5f) }
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text("Brightness: ${sliderPosition.roundToInt()}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..10f,
            steps = 9,
            onValueChangeFinished = {
                val command = "set brightness to ${sliderPosition.roundToInt()}"
                viewModel.sendButtonCommand(command)
            },
            thumb = {
                Icon(
                    imageVector = Icons.Default.BrightnessMedium,
                    contentDescription = "Brightness",
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}