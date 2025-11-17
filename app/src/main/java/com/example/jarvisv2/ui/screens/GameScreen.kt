package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
                .height(500.dp), // Stays large enough for usage
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trackpad",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // --- Touch Surface ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        // Clicks
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { viewModel.sendButtonCommand("mouse click left") },
                                onLongPress = { viewModel.sendButtonCommand("mouse click right") }
                            )
                        }
                        // Movement
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                // --- SENSITIVITY CONTROL ---
                                // 2.5f = Fast | 1.0f = Normal | 0.5f = Slow
                                val sensitivity = 1.0f

                                val dx = (dragAmount.x * sensitivity).roundToInt()
                                val dy = (dragAmount.y * sensitivity).roundToInt()

                                if (kotlin.math.abs(dx) > 0 || kotlin.math.abs(dy) > 0) {
                                    viewModel.sendButtonCommand("trackpad move $dx $dy")
                                }
                            }
                        }
                ) {
                    Text(
                        "Swipe to Move\nTap to Click",
                        color = DarkPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}