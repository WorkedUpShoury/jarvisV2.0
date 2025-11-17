package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.viewmodel.MainViewModel

private val webActions = listOf(
    // --- Sites (Using System Icons for now to fix build) ---
    Action("LinkedIn", Icons.Default.Business, "open linkedin"),
    Action("ChatGPT",  Icons.Default.SmartToy, "open chat"),
    Action("Gemini",   Icons.Default.AutoAwesome, "open gemini"),
    Action("Gmail",    Icons.Default.Email, "open gmail"),
    Action("Drive",    Icons.Default.Cloud, "open drive"),
    Action("GitHub",   Icons.Default.Code, "open github"),
    Action("LeetCode", Icons.Default.Terminal, "open leetcode"),
    Action("StackOvfl", Icons.Default.Help, "open stackoverflow"),

    // --- Utilities ---
    Action("News",     Icons.Default.Newspaper, "get news"),
    Action("Weather",  Icons.Default.WbSunny,   "get weather"),
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
            ActionButton(
                icon = action.icon,
                iconDrawable = action.iconDrawable,
                text = action.name,
                viewModel = viewModel,
                command = action.command
            )
        }
    }
}