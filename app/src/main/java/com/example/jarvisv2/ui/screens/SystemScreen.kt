package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.viewmodel.MainViewModel
import kotlin.math.roundToInt

// --- 1. DEFINITIONS SPLIT INTO TWO LISTS ---

// List 1: For the new horizontal row card
private val windowRowActions = listOf(
    Action("Minimize", Icons.Default.Minimize, "minimize window"),
    Action("Fullscreen", Icons.Default.Fullscreen, "fullscreen"), // Using Fullscreen for "maximize"
    Action("Close", Icons.Default.Close, "close window")
)

// List 2: For the existing vertical grid
private val systemGridActions = listOf(
    Action("Lock", Icons.Default.Lock, "lock screen"),
    Action("Shutdown", Icons.Default.PowerSettingsNew, "shutdown"),
    Action("Restart", Icons.Default.RestartAlt, "restart"),
    Action("Sleep", Icons.Default.Bedtime, "sleep system"),
)

// --- 3. SystemScreen COMPOSABLE UPDATED ---
@Composable
fun SystemScreen(viewModel: MainViewModel) {
    // --- WRAP THE SCREEN IN A COLUMN ---
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- ADD THE NEW MEDIA SEARCH INPUT AT THE TOP ---
        MediaSearchInput(viewModel = viewModel)

        // --- 4. NEW CARD ADDED HERE ---
        WindowActionsCard(viewModel = viewModel)

        // --- THE EXISTING GRID IS NOW THE SECOND ITEM IN THE COLUMN ---
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

            // --- 5. GRID UPDATED TO USE THE NEW LIST ---
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

// --- 2. NEW COMPOSABLE FOR THE HORIZONTAL CARD ---
@Composable
private fun WindowActionsCard(viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Iterate over the new list
            windowRowActions.forEach { action ->
                // Create a small, clickable column for each button
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


// --- NEW COMPOSABLE FOR THE MEDIA SEARCH BAR ---
@Composable
fun MediaSearchInput(viewModel: MainViewModel) {
    var text by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("spotify") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- TOGGLE BUTTONS FOR SPOTIFY/YOUTUBE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Spotify Button
            OutlinedButton(
                onClick = { selectedSource = "spotify" },
                modifier = Modifier.weight(1f),
                colors = if (selectedSource == "spotify") {
                    ButtonDefaults.outlinedButtonColors(containerColor = DarkPrimary.copy(alpha = 0.3f))
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("Spotify")
            }

            // YouTube Button
            OutlinedButton(
                onClick = { selectedSource = "youtube" },
                modifier = Modifier.weight(1f),
                colors = if (selectedSource == "youtube") {
                    ButtonDefaults.outlinedButtonColors(containerColor = DarkPrimary.copy(alpha = 0.3f))
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("YouTube")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- TEXT INPUT BAR ---
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter song name...") },
            trailingIcon = {
                IconButton(onClick = {
                    val query = text.trim()
                    if (query.isNotEmpty()) {
                        // Construct the command based on the selected source
                        val command = if (selectedSource == "spotify") {
                            "play $query on spotify"
                        } else {
                            "play $query" // This is the command for YouTube
                        }

                        // Use the silent button command function
                        viewModel.sendButtonCommand(command)

                        text = "" // Clear text
                        focusManager.clearFocus() // Hide keyboard
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = DarkPrimary
                    )
                }
            },
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedBorderColor = DarkPrimary,
                unfocusedBorderColor = Color.Gray,
                cursorColor = DarkPrimary
            ),
            singleLine = true
        )
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