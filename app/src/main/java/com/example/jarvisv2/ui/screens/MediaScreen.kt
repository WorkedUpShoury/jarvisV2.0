// workedupshoury/jarvisv2.0/jarvisV2.0-aaa92dd1e8476ce67109495778760087eb2dcc1d/app/src/main/java/com/example/jarvisv2/ui/screens/MediaScreen.kt
package com.example.jarvisv2.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <--- NEW IMPORT
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // <--- CHANGED FROM GRID TO COLUMN for outer layout
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star // <--- NEW ICON IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight // <--- NEW IMPORT
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.network.MediaStateResponse
import com.example.jarvisv2.data.MediaSearch // <--- NEW IMPORT
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel

@Composable
fun MediaScreen(viewModel: MainViewModel) {
    val mediaState by viewModel.mediaState.collectAsState()
    // REMOVED: showTrackpad and showKeyboard states
    // REMOVED: TrackpadDialog and KeyboardDialog calls

    // Use LazyColumn for the main screen layout to allow scrolling all sections
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 100.dp) // Extra padding for navbar
    ) {
        // 1. Now Playing Card
        item {
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
        }

        // 2. Search Input
        item {
            MediaSearchInput(viewModel)
        }

        // 3. Search History (New Content)
        item {
            MediaSearchHistory(viewModel = viewModel)
        }

        // REMOVED: 4. Control Grid (Trackpad/Keyboard)
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
                        // 1. Save the search
                        viewModel.saveMediaSearch(query, selectedSource) // <--- NEW SAVE CALL
                        // 2. Play it
                        viewModel.playMediaSearch(query, selectedSource)
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

// --- NEW COMPOSABLE FOR SEARCH HISTORY ---
@Composable
fun MediaSearchHistory(viewModel: MainViewModel) {
    val mostSearched by viewModel.mostSearchedQuery.collectAsState()
    val recentSearches by viewModel.recentMediaSearches.collectAsState()

    // Filter out the most searched item from the recent list to avoid duplication
    val filteredRecent = remember(recentSearches, mostSearched) {
        // Filter out if query and source match
        recentSearches.filter { search ->
            mostSearched == null || search.query != mostSearched!!.query || search.source != mostSearched!!.source
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {

        // Most Searched Query (Top)
        if (mostSearched != null) {
            Text(
                "Most Searched",
                style = MaterialTheme.typography.titleSmall,
                color = DarkPrimary,
                fontWeight = FontWeight.SemiBold
            )
            SearchItemCard(search = mostSearched!!, isMostSearched = true) {
                // Clicking plays the item
                viewModel.playMediaSearch(mostSearched!!.query, mostSearched!!.source)
                viewModel.saveMediaSearch(mostSearched!!.query, mostSearched!!.source) // Log the re-play as a search
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent Searches List (Below)
        if (filteredRecent.isNotEmpty()) {
            Text(
                "Recent Searches",
                style = MaterialTheme.typography.titleSmall,
                color = DarkOnSurface,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredRecent.forEach { search ->
                    SearchItemCard(search = search, isMostSearched = false) {
                        // Clicking plays the item
                        viewModel.playMediaSearch(search.query, search.source)
                        viewModel.saveMediaSearch(search.query, search.source) // Log the re-play as a search
                    }
                }
            }
        }
    }
}

// Helper Composable for the clickable search item
@Composable
fun SearchItemCard(search: MediaSearch, isMostSearched: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF262626) // A slightly different dark surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isMostSearched) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Most Searched",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Icon",
                        tint = DarkPrimary,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    )
                }

                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = search.query,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Source: ${search.source.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Show the count badge if not null
            if (search.count > 0) {
                Badge(
                    containerColor = DarkPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(text = search.count.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}