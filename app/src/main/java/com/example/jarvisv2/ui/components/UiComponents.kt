package com.example.jarvisv2.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.service.JarvisVoiceService
import com.example.jarvisv2.ui.theme.DarkOnPrimary
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.ChatMessage
import com.example.jarvisv2.viewmodel.ChatSender
import kotlinx.coroutines.flow.StateFlow

// ============================================================================
//  CONNECTION STATUS ICON
// ============================================================================
@Composable
fun ConnectionStatusIcon(
    serverUrlFlow: StateFlow<String?>,
    isDiscoveringFlow: StateFlow<Boolean>
) {
    val serverUrl by serverUrlFlow.collectAsState()
    val isDiscovering by isDiscoveringFlow.collectAsState()

    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .size(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isDiscovering) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = DarkPrimary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (serverUrl != null) Color(0xFF00E676) else Color.Red)
                )
            }
        }
    }
}

// ============================================================================
//  VOICE SERVICE TOGGLE BUTTON (FIXED ENUM COMPARISON)
// ============================================================================
@Composable
fun VoiceServiceToggleButton(
    isRunningFlow: StateFlow<JarvisVoiceService.Companion.ServiceState>,
    onToggle: () -> Unit
) {
    val state by isRunningFlow.collectAsState()

    IconButton(onClick = onToggle) {
        Crossfade(targetState = state) { current ->
            if (current == JarvisVoiceService.Companion.ServiceState.Running) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Stop service",
                    tint = DarkPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Start service",
                    tint = Color.Gray
                )
            }
        }
    }
}

// ============================================================================
//  CHAT BUBBLE
// ============================================================================
@Composable
fun ChatBubble(chat: ChatMessage) {
    val isUser = chat.sender == ChatSender.User

    val bubbleColor =
        if (isUser) DarkPrimary else DarkSurface

    val textColor =
        if (isUser) DarkOnPrimary else DarkOnSurface

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = chat.message,
            color = textColor,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 300.dp)
        )
    }
}

// ============================================================================
//  COMMAND INPUT BAR
// ============================================================================
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
            .padding(8.dp)
    ) {

        // SUGGESTIONS
        if (suggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { sug ->
                    SuggestionChip("/$sug") {
                        onSuggestionClick(sug)
                    }
                }
            }
        }

        // INPUT FIELD
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type / for commands…") },
            trailingIcon = {
                IconButton(onClick = {
                    onSend()
                    focusManager.clearFocus()
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

// ============================================================================
//  SUGGESTION CHIP
// ============================================================================
@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color.Black,
        border = BorderStroke(1.dp, DarkPrimary)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = DarkPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
