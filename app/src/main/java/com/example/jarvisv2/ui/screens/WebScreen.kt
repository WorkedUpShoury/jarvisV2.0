package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.viewmodel.MainViewModel

private val webActions = listOf(
    Action("Next", Icons.Default.SkipNext, "next track"),
    Action("Previous", Icons.Default.SkipPrevious, "previous track"),
    Action("Pause", Icons.Default.Pause, "pause playback"),
    Action("Resume", Icons.Default.PlayArrow, "resume playback"),
    Action("Switch Tab", Icons.Default.SwapHoriz, "alt tab"),
    Action("Next Tab", Icons.Default.DynamicFeed, "ctrl tab"),
    Action("News", Icons.Default.Newspaper, "get news"),
    Action("Weather", Icons.Default.WbSunny, "get weather"),
    Action("Time", Icons.Default.AccessTime, "what is the time"),
    Action("Date", Icons.Default.CalendarMonth, "what is the date"),
)

@Composable
fun WebScreen(viewModel: MainViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(webActions) { action ->
            // --- UPDATED to use the new ActionButton overload ---
            ActionButton(
                icon = action.icon,
                text = action.name,
                viewModel = viewModel,
                command = action.command
            )
        }
    }
}