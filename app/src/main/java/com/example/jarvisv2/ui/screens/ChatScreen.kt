package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.* // Ensures Material 3 usage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.ui.components.ChatBubble
import com.example.jarvisv2.ui.components.CommandInputBar
import com.example.jarvisv2.ui.theme.DarkError
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.ChatSender
import com.example.jarvisv2.viewmodel.MainViewModel

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val commandText by viewModel.commandText.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    val listState = rememberLazyListState()

    // State for Dialogs and Menus
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Auto-scroll when new chat arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // --- Clear All Confirmation Dialog ---
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear Chat?") },
            text = { Text("This will delete all message history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChatHistory()
                    showClearAllDialog = false
                }) {
                    Text("Clear All", color = DarkError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = DarkOnSurface)
                }
            },
            containerColor = DarkSurface,
            titleContentColor = DarkOnSurface,
            textContentColor = DarkOnSurface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // --- Header Row with Three-Dot Menu ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chat History",
                color = Color.White,
                fontSize = 18.sp
            )

            // Menu Box
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    // REMOVED: containerColor parameter to fix the error.
                    // The theme's default surface color (DarkSurface) will be used automatically.
                ) {
                    DropdownMenuItem(
                        text = { Text("Clear History", color = DarkError) },
                        onClick = {
                            showMenu = false
                            showClearAllDialog = true
                        }
                    )
                }
            }
        }

        // --- Chat List ---
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { chat ->
                ChatBubble(
                    chat = chat,
                    onDelete = { viewModel.deleteChatMessage(chat) },
                    onRepeat = if (chat.sender == ChatSender.User) {
                        // Only allow repeating user commands
                        { viewModel.sendButtonCommand(chat.message) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Input Bar ---
        CommandInputBar(
            text = commandText,
            onTextChanged = viewModel::onCommandTextChanged,
            onSend = viewModel::sendCurrentCommand,
            suggestions = suggestions,
            onSuggestionClick = {
                viewModel.onCommandTextChanged(it)
            }
        )
    }
}