package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.jarvisv2.viewmodel.MainViewModel

/**
 * Media quick-play dialog with 3 large buttons:
 * Spotify, YouTube, and Liked Music.
 *
 * Uses the shared ActionButton component.
 */
@Composable
fun PlayDialog(viewModel: MainViewModel) {
    Dialog(onDismissRequest = { viewModel.onPlayDialogDismiss() }) {

        Card(
            shape = CardDefaults.shape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Play Media",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    ActionButton(
                        icon = Icons.Default.MusicNote,
                        text = "Spotify",
                        onClick = {
                            viewModel.sendCommand("play on spotify")
                            viewModel.onPlayDialogDismiss()
                        }
                    )

                    ActionButton(
                        icon = Icons.Default.VideoLibrary,
                        text = "YouTube",
                        onClick = {
                            viewModel.sendCommand("play on youtube")
                            viewModel.onPlayDialogDismiss()
                        }
                    )

                    ActionButton(
                        icon = Icons.Default.Favorite,
                        text = "Liked",
                        onClick = {
                            viewModel.sendCommand("play liked songs")
                            viewModel.onPlayDialogDismiss()
                        }
                    )
                }
            }
        }
    }
}
