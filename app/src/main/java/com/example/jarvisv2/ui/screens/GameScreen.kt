package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun TrackpadDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(24.dp), // Consistent with Keyboard
            color = DarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // --- Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trackpad Control",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DarkOnSurface
                        )
                    }
                }

                // --- Touch Surface ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = Color.Black.copy(alpha = 0.5f), // Matches Keyboard Input Field
                            shape = RoundedCornerShape(16.dp)
                        )
                        // Clicks
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    viewModel.sendUdpCommand("clk|left")
                                },
                                onLongPress = {
                                    viewModel.sendUdpCommand("clk|right")
                                }
                            )
                        }
                        // Movement
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val sensitivity = 1.5f
                                val dx = (dragAmount.x * sensitivity).roundToInt()
                                val dy = (dragAmount.y * sensitivity).roundToInt()
                                if (kotlin.math.abs(dx) > 0 || kotlin.math.abs(dy) > 0) {
                                    viewModel.sendUdpCommand("mov|$dx,$dy")
                                }
                            }
                        }
                ) {
                    Text(
                        "Swipe to Move\nTap: Left Click | Hold: Right Click",
                        color = DarkPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Scroll Controls (Matches KeyButton Style) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scroll Up Button
                    ScrollButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        description = "Scroll Up",
                        modifier = Modifier.weight(1f)
                    ) { viewModel.sendUdpCommand("scr|3") }

                    // Scroll Down Button
                    ScrollButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        description = "Scroll Down",
                        modifier = Modifier.weight(1f)
                    ) { viewModel.sendUdpCommand("scr|-3") }
                }
            }
        }
    }
}

@Composable
fun ScrollButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp), // Matches KeyButton height
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkPrimary.copy(alpha = 0.3f)), // Matches KeyButton border
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = DarkSurface,
            contentColor = DarkPrimary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(28.dp)
        )
    }
}