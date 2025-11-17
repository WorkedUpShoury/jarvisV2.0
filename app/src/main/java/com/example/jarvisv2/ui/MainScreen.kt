package com.example.jarvisv2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.data.quickCommands
import com.example.jarvisv2.service.JarvisVoiceService
import com.example.jarvisv2.service.JarvisVoiceService.Companion.ServiceState
import com.example.jarvisv2.ui.components.VoiceStatusIcon
import com.example.jarvisv2.ui.theme.DarkError
import com.example.jarvisv2.ui.theme.DarkOnPrimary
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.ui.theme.SystemMessage
// --- UPDATED IMPORTS ---
import com.example.jarvisv2.data.ChatMessage
import com.example.jarvisv2.viewmodel.ChatSender
import com.example.jarvisv2.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onToggleVoiceService: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val commandText by viewModel.commandText.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    val isVoiceServiceRunning by viewModel.isVoiceServiceRunning.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jarvis V2 Control") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = DarkOnSurface
                ),
                actions = {
                    VoiceStatusIcon(
                        detailedStateFlow = viewModel.detailedVoiceState,
                        onToggle = onToggleVoiceService
                    )
                },
                navigationIcon = {
                    ConnectionStatusIcon(serverUrl, isDiscovering)
                }
            )
        },
        bottomBar = {
            CommandInputBar(
                text = commandText,
                onTextChanged = viewModel::onCommandTextChanged,
                onSend = viewModel::sendCurrentCommand,
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    viewModel.onCommandTextChanged(suggestion)
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = "Quick Commands",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { command ->
                    QuickCommandButton(
                        command = command.description,
                        icon = command.icon
                    ) {
                        viewModel.sendButtonCommand(command.command)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatHistory) { chat ->
                    ChatBubble(chat)
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusIcon(serverUrl: String?, isDiscovering: Boolean) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isDiscovering, label = "ConnectionStatus") { discovering ->
            if (discovering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.Yellow
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (serverUrl != null) Color.Green else Color.Red)
                )
            }
        }
    }
}

@Composable
fun VoiceServiceToggleButton(isRunning: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Crossfade(targetState = isRunning, label = "VoiceToggle") { running ->
            if (running) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Stop Voice Service",
                    tint = DarkPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Start Voice Service",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun QuickCommandButton(command: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(70.dp)
                .padding(vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = command,
                    tint = DarkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = command,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun ChatBubble(chat: ChatMessage) {
    val alignment =
        if (chat.sender == ChatSender.User) Alignment.CenterEnd
        else Alignment.CenterStart

    val backgroundColor =
        if (chat.sender == ChatSender.User) DarkPrimary else DarkSurface

    val textColor =
        if (chat.sender == ChatSender.User) DarkOnPrimary
        else if (chat.message.startsWith("Error:")) DarkError
        else DarkOnSurface

    val textStyle =
        if (chat.sender == ChatSender.System)
            MaterialTheme.typography.bodySmall.copy(color = SystemMessage)
        else MaterialTheme.typography.bodyMedium

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Text(
            text = chat.message,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 300.dp),
            color = textColor,
            style = textStyle
        )
    }
}

@Composable
fun CommandInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .navigationBarsPadding()
            .imePadding()
    ) {

        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(suggestion) {
                        onSuggestionClick("/$suggestion ")
                    }
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            placeholder = { Text("Type / for commands or chat...") },
            trailingIcon = {
                IconButton(
                    onClick = {
                        onSend()
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = DarkPrimary
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkPrimary,
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    onSend()
                    focusManager.clearFocus()
                }
            ),
            singleLine = true
        )
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, DarkPrimary)
    ) {
        Text(
            text = "/$text",
            color = DarkPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}