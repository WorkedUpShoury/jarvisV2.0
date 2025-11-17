package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.viewmodel.MainViewModel

@Composable
fun MediaScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Search / Input Section
        MediaSearchInput(viewModel)

        // 2. Media Controls Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ActionButton(icon = Icons.Default.SkipPrevious, text = "Prev", viewModel = viewModel, command = "previous track")
            }
            item {
                ActionButton(icon = Icons.Default.PlayArrow, text = "Play", viewModel = viewModel, command = "resume playback")
            }
            item {
                ActionButton(icon = Icons.Default.Pause, text = "Pause", viewModel = viewModel, command = "pause playback")
            }
            item {
                ActionButton(icon = Icons.Default.SkipNext, text = "Next", viewModel = viewModel, command = "next track")
            }
        }
    }
}

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
        Text(
            text = "Play Media",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter song name...") },
            trailingIcon = {
                IconButton(onClick = {
                    val query = text.trim()
                    if (query.isNotEmpty()) {
                        val command = if (selectedSource == "spotify") {
                            "play $query on spotify"
                        } else {
                            "play $query"
                        }
                        viewModel.sendButtonCommand(command)
                        text = ""
                        focusManager.clearFocus()
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