package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel

private val webActions = listOf(
    // --- Sites ---
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
        // Added generous bottom padding so the last row isn't hidden behind the nav bar
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. Browser Controls as a scrollable Header ---
        // This moves it INSIDE the scrollable area so it doesn't eat up screen space
        item(span = { GridItemSpan(maxLineSpan) }) {
            BrowserControlCard(viewModel)
        }

        // --- 2. Quick Links Grid ---
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

@Composable
fun BrowserControlCard(viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp), // Padding inside the grid item
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Browser Controls",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back
                NavIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    desc = "Back",
                    onClick = { viewModel.sendButtonCommand("browser back") }
                )
                // Forward
                NavIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    desc = "Forward",
                    onClick = { viewModel.sendButtonCommand("browser forward") }
                )
                // Refresh
                NavIconButton(
                    icon = Icons.Default.Refresh,
                    desc = "Refresh",
                    onClick = { viewModel.sendButtonCommand("browser refresh") }
                )
                // New Tab
                NavIconButton(
                    icon = Icons.Default.Add,
                    desc = "New Tab",
                    onClick = { viewModel.sendButtonCommand("browser new tab") }
                )
                // Close Tab
                NavIconButton(
                    icon = Icons.Default.Close,
                    desc = "Close Tab",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { viewModel.sendButtonCommand("browser close tab") }
                )
                // Switch Tab (REPLACED Reopen Tab)
                NavIconButton(
                    icon = Icons.Default.SwapHoriz,
                    desc = "Switch Tab",
                    onClick = { viewModel.sendButtonCommand("ctrl tab") } // Command for switching tabs
                )
            }
        }
    }
}

@Composable
fun NavIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    color: Color = DarkPrimary,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.3f),
            contentColor = color
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(20.dp)
        )
    }
}