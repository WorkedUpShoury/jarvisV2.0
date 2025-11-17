package com.example.jarvisv2.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.viewmodel.MainViewModel
import kotlin.math.roundToInt

// --- NEW Blue Gradient and Button Palette ---
val DarkBlueGradientStart = Color(0xFF000022) // Very dark blue for the top
val DarkBlueGradientEnd = Color(0xFF000044)   // Slightly lighter dark blue for the bottom

val ColorCyanLight = Color(0xFF29B6F6)   // Lighter blue for primary actions (Fire, Jump)
val ColorBluePrimary = Color(0xFF42A5F5) // Standard blue for WASD, common actions
val ColorIndigoDark = Color(0xFF3F51B5)  // Darker indigo for secondary/less frequent actions (Crouch, X)
val ColorBlueGray = Color(0xFF78909C)    // Muted blue-gray for utility (Reload, Q, E)
val ColorWhiteTransparent = Color(0x80FFFFFF) // Still useful for active/inactive toggles

@Composable
fun GameScreen(viewModel: MainViewModel) {
    // 1. Force Landscape Mode
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    var isWalkMode by remember { mutableStateOf(false) } // State for Sprint/Walk toggle

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colors = listOf(DarkBlueGradientStart, DarkBlueGradientEnd)
            ))
    ) {
        // =====================================================================
        // LAYER 1: AIM TRACKPAD (Right Half Background)
        // =====================================================================
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f) // Covers right side
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val sensitivity = 3.0f
                        val dx = (dragAmount.x * sensitivity).roundToInt()
                        val dy = (dragAmount.y * sensitivity).roundToInt()
                        if (kotlin.math.abs(dx) > 1 || kotlin.math.abs(dy) > 1) {
                            viewModel.sendButtonCommand("trackpad move $dx $dy")
                        }
                    }
                }
        ) {
            Text(
                "AIM ZONE",
                color = ColorBluePrimary.copy(alpha = 0.2f), // Lighter blue for visibility
                modifier = Modifier.align(Alignment.Center),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // =====================================================================
        // LAYER 2: CONTROLS
        // =====================================================================

        // --- TOP LEFT: WEAPONS (1, 2, 3) & Utilities (Mute, Buy) ---
        // Aligned TopStart, with individual offsets for precise placement from screenshot
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 24.dp)
        ) {
            // Weapon 1
            GameIconButton(text = "1", pressCommand = "game press 1", viewModel = viewModel, size = 45, color = ColorBluePrimary)
            // Weapon 2
            GameIconButton(text = "2", pressCommand = "game press 2", viewModel = viewModel, size = 45, color = ColorBluePrimary,
                modifier = Modifier.offset(x = 60.dp))
            // Weapon 3
            GameIconButton(text = "3", pressCommand = "game press 3", viewModel = viewModel, size = 45, color = ColorBluePrimary,
                modifier = Modifier.offset(x = 120.dp))

            // Mute Button (Above 1)
            GameIconButton(icon = Icons.Default.VolumeOff, pressCommand = "game mute", viewModel = viewModel, size = 45, color = ColorBlueGray,
                modifier = Modifier.offset(y = -60.dp))

            // Buy Button (Below Mute)
            GameIconButton(
                icon = Icons.Default.ShoppingCart,
                pressCommand = "game press b",
                viewModel = viewModel,
                size = 45,
                color = ColorBlueGray,
                modifier = Modifier.offset(y = 60.dp)
            )
        }

        // --- BOTTOM LEFT: MOVEMENT CLUSTER ---
        // Adjusted to match the screenshot's dense, slightly offset arrangement
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-40).dp)
                .size(250.dp) // Maintain a conceptual bounding box for relative positioning
        ) {
            val dPadOffset = 60.dp // Base offset for D-pad elements
            val utilOffset = 70.dp // Offset for Q,E,X,C

            // WASD (Arrows) - Main Movement
            GameIconButton(
                icon = Icons.Default.KeyboardArrowUp,
                pressCommand = "game hold w", viewModel = viewModel, releaseCommand = "game release w",
                modifier = Modifier.align(Alignment.Center).offset(y = -dPadOffset),
                color = ColorBluePrimary
            )
            GameIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                pressCommand = "game hold s", viewModel = viewModel, releaseCommand = "game release s",
                modifier = Modifier.align(Alignment.Center).offset(y = dPadOffset),
                color = ColorBluePrimary
            )
            GameIconButton(
                icon = Icons.Default.KeyboardArrowLeft,
                pressCommand = "game hold a", viewModel = viewModel, releaseCommand = "game release a",
                modifier = Modifier.align(Alignment.Center).offset(x = -dPadOffset),
                color = ColorBluePrimary
            )
            GameIconButton(
                icon = Icons.Default.KeyboardArrowRight,
                pressCommand = "game hold d", viewModel = viewModel, releaseCommand = "game release d",
                modifier = Modifier.align(Alignment.Center).offset(x = dPadOffset),
                color = ColorBluePrimary
            )

            // UTILS (Q, E, X, C) - Placed to visually overlap slightly with WASD as in screenshot
            GameIconButton(text = "Q", pressCommand = "game press q", viewModel = viewModel, size = 45, color = ColorBlueGray,
                modifier = Modifier.align(Alignment.Center).offset(x = -utilOffset - 10.dp, y = -utilOffset - 10.dp))

            GameIconButton(text = "E", pressCommand = "game press e", viewModel = viewModel, size = 45, color = ColorBlueGray,
                modifier = Modifier.align(Alignment.Center).offset(x = utilOffset + 10.dp, y = -utilOffset - 10.dp))

            GameIconButton(text = "X", pressCommand = "game press x", viewModel = viewModel, size = 45, color = ColorIndigoDark,
                modifier = Modifier.align(Alignment.Center).offset(x = -utilOffset - 10.dp, y = utilOffset + 10.dp))

            GameIconButton(text = "C", pressCommand = "game press c", viewModel = viewModel, size = 45, color = ColorIndigoDark,
                modifier = Modifier.align(Alignment.Center).offset(x = utilOffset + 10.dp, y = utilOffset + 10.dp))

            // RELOAD (R) - Positioned to the right of E, as in screenshot
            GameIconButton(text = "R", pressCommand = "game press r", viewModel = viewModel, size = 45, color = ColorBlueGray,
                modifier = Modifier.align(Alignment.Center).offset(x = utilOffset * 2.1f, y = -utilOffset - 10.dp))

            // FIRE Button - Left-side placement, slightly below center
            GameIconButton(
                icon = Icons.Default.TouchApp,
                pressCommand = "game fire",
                viewModel = viewModel,
                size = 60,
                color = ColorCyanLight,
                modifier = Modifier.align(Alignment.Center).offset(y = utilOffset * 1.8f) // Adjusted Y offset
            )
        }

        // --- RIGHT SIDE CONTROLS ---
        // Jump, Scope, Sprint (Shift)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 40.dp, end = 40.dp)
                .size(220.dp)
        ) {
            // JUMP (Green Arrow) - Bottom Right
            GameIconButton(
                icon = Icons.Default.ArrowForward, // Changed to forward arrow as in screenshot
                pressCommand = "game press ctrl",
                viewModel = viewModel,
                size = 65,
                color = ColorBluePrimary,
                modifier = Modifier.align(Alignment.CenterStart) // Left side of this box
            )

            // SCOPE (White Target) - Top of this cluster
            GameIconButton(
                icon = Icons.Default.CenterFocusStrong,
                pressCommand = "game scope",
                viewModel = viewModel,
                size = 60,
                color = ColorCyanLight,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // SPRINT (Shift) - Moved to this cluster, bottom-center for right thumb
            GameIconButton(
                icon = if (isWalkMode) Icons.Default.DirectionsWalk else Icons.Default.DirectionsRun,
                pressCommand = "",
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomCenter),
                size = 55,
                color = if (isWalkMode) ColorCyanLight else ColorWhiteTransparent,
                isToggle = true,
                onToggle = {
                    isWalkMode = !isWalkMode
                    if (isWalkMode) viewModel.sendButtonCommand("game hold shift")
                    else viewModel.sendButtonCommand("game release shift")
                }
            )
        }
    }
}

/**
 * GameIconButton Composable
 * Supports displaying an icon or a text string.
 */
@Composable
fun GameIconButton(
    icon: ImageVector? = null,
    text: String? = null,
    pressCommand: String,
    viewModel: MainViewModel,
    releaseCommand: String? = null,
    modifier: Modifier = Modifier,
    size: Int = 55,
    color: Color = ColorBluePrimary, // Default to a primary blue
    isToggle: Boolean = false,
    onToggle: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isToggle) {
                            onToggle()
                        } else {
                            try {
                                if (pressCommand.isNotEmpty()) {
                                    viewModel.sendButtonCommand(pressCommand)
                                    if (releaseCommand != null) {
                                        tryAwaitRelease()
                                        viewModel.sendButtonCommand(releaseCommand)
                                    }
                                }
                            } catch (e: Exception) { /* ignore */ }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size * 0.45).sp
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size((size * 0.5).dp)
            )
        }
    }
}