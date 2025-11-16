package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jarvisv2.ui.components.ChatBubble
import com.example.jarvisv2.ui.components.CommandInputBar
import com.example.jarvisv2.viewmodel.MainViewModel

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val commandText by viewModel.commandText.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll when new chat arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Chat messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { chat ->
                ChatBubble(chat)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
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
