package com.example.jarvisv2.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.network.MediaStateResponse
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel

@Composable
fun MediaScreen(viewModel: MainViewModel) {
    val mediaState by viewModel.mediaState.collectAsState()
    var showTrackpad by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }

    if (showTrackpad) TrackpadDialog(viewModel) { showTrackpad = false }
    if (showKeyboard) KeyboardDialog(viewModel) { showKeyboard = false }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Now Playing Card
        if (mediaState != null) {
            NowPlayingCard(
                state = mediaState!!,
                onPlayPause = {
                    val cmd = if (mediaState!!.is_playing) "pause playback" else "resume playback"
                    viewModel.sendButtonCommand(cmd)
                },
                onNext = { viewModel.sendButtonCommand("next track") },
                onPrev = { viewModel.sendButtonCommand("previous track") }
            )
        }

        // 2. Search Input
        MediaSearchInput(viewModel)

        // 3. Control Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ActionButton(
                    icon = Icons.Default.Mouse,
                    text = "Trackpad",
                    containerColor = DarkSurface,
                    onClick = { showTrackpad = true }
                )
            }
            item {
                ActionButton(
                    icon = Icons.Default.Keyboard,
                    text = "Keyboard",
                    containerColor = DarkSurface,
                    onClick = { showKeyboard = true }
                )
            }
            item { ActionButton(icon = Icons.Default.SkipPrevious, text = "Prev", viewModel = viewModel, command = "previous track") }
            item { ActionButton(icon = Icons.Default.PlayArrow, text = "Play", viewModel = viewModel, command = "resume playback") }
            item { ActionButton(icon = Icons.Default.Pause, text = "Pause", viewModel = viewModel, command = "pause playback") }
            item { ActionButton(icon = Icons.Default.SkipNext, text = "Next", viewModel = viewModel, command = "next track") }
        }
    }
}

@Composable
fun NowPlayingCard(
    state: MediaStateResponse,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(140.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageBitmap = remember(state.thumbnail) {
                try {
                    if (state.thumbnail != null) {
                        val decodedString = Base64.decode(state.thumbnail, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size).asImageBitmap()
                    } else null
                } catch (e: Exception) { null }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap, contentDescription = "Album Art",
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.MusicNote, "No Art", tint = Color.White) }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.title, style = MaterialTheme.typography.titleMedium, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artist, style = MaterialTheme.typography.bodyMedium, color = Color.Gray,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrev) { Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White) }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.background(Color.White, CircleShape).size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (state.is_playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause", tint = Color.Black
                        )
                    }
                    IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White) }
                }
            }
        }
    }
}

@Composable
fun MediaSearchInput(viewModel: MainViewModel) {
    var text by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("spotify") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Search Media", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { selectedSource = "spotify" },
                modifier = Modifier.weight(1f),
                colors = if (selectedSource == "spotify") ButtonDefaults.outlinedButtonColors(containerColor = DarkPrimary.copy(alpha = 0.3f)) else ButtonDefaults.outlinedButtonColors()
            ) { Text("Spotify") }
            OutlinedButton(
                onClick = { selectedSource = "youtube" },
                modifier = Modifier.weight(1f),
                colors = if (selectedSource == "youtube") ButtonDefaults.outlinedButtonColors(containerColor = DarkPrimary.copy(alpha = 0.3f)) else ButtonDefaults.outlinedButtonColors()
            ) { Text("YouTube") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter song name...") },
            trailingIcon = {
                IconButton(onClick = {
                    val query = text.trim()
                    if (query.isNotEmpty()) {
                        val command = if (selectedSource == "spotify") "play $query on spotify" else "play $query"
                        viewModel.sendButtonCommand(command)
                        text = ""
                        focusManager.clearFocus()
                    }
                }) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = DarkPrimary) }
            },
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black,
                focusedBorderColor = DarkPrimary, cursorColor = DarkPrimary
            ),
            singleLine = true
        )
    }
}