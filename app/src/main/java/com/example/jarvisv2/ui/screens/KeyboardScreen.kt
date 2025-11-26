package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jarvisv2.ui.theme.DarkError
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel

@Composable
fun KeyboardDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSwitchToTrackpad: () -> Unit // <--- NEW CALLBACK
) {
    var textInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. Header ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keyboard Control",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )

                    Row {
                        // Switch to Trackpad Button
                        IconButton(onClick = onSwitchToTrackpad) {
                            Icon(
                                imageVector = Icons.Default.Mouse,
                                contentDescription = "Switch to Trackpad",
                                tint = DarkPrimary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = DarkOnSurface)
                        }
                    }
                }

                // --- 2. Input Field ---
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Type text to send...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = DarkPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        // Send Button
                        FilledIconButton(
                            onClick = {
                                if (textInput.isNotEmpty()) {
                                    viewModel.sendButtonCommand("type $textInput")
                                    textInput = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = DarkPrimary),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                Divider(color = Color.White.copy(alpha = 0.1f))

                // --- 3. Function Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyButton(text = "Esc", modifier = Modifier.weight(1f)) { viewModel.sendUdpCommand("tap|esc") }
                    KeyButton(text = "Tab", modifier = Modifier.weight(1f)) { viewModel.sendUdpCommand("tap|tab") }
                    KeyButton(text = "Caps", modifier = Modifier.weight(1f)) { viewModel.sendUdpCommand("tap|capslock") }
                    KeyButton(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        modifier = Modifier.weight(1f),
                        isDestructive = true
                    ) { viewModel.sendUdpCommand("tap|backspace") }
                }

                // --- 4. Navigation Cluster ---
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeyButton(icon = Icons.Default.KeyboardArrowUp, size = 56.dp) { viewModel.sendUdpCommand("tap|up") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KeyButton(icon = Icons.Default.KeyboardArrowLeft, size = 56.dp) { viewModel.sendUdpCommand("tap|left") }
                            KeyButton(icon = Icons.Default.KeyboardArrowDown, size = 56.dp) { viewModel.sendUdpCommand("tap|down") }
                            KeyButton(icon = Icons.Default.KeyboardArrowRight, size = 56.dp) { viewModel.sendUdpCommand("tap|right") }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // --- 5. Bottom Row (Space & Enter) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KeyButton(
                        text = "Space",
                        modifier = Modifier.weight(2f).height(56.dp)
                    ) { viewModel.sendUdpCommand("tap|space") }

                    Button(
                        onClick = { viewModel.sendUdpCommand("tap|enter") },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("Enter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    text: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isDestructive) {
        DarkError.copy(alpha = 0.15f)
    } else {
        DarkSurface
    }

    val borderColor = if (isDestructive) {
        DarkError.copy(alpha = 0.5f)
    } else {
        DarkPrimary.copy(alpha = 0.3f)
    }

    val contentColor = if (isDestructive) {
        DarkError
    } else {
        DarkPrimary
    }

    val finalModifier = if (size != null) modifier.size(size) else modifier.height(48.dp)

    OutlinedButton(
        onClick = onClick,
        modifier = finalModifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (text != null) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}