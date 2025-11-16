package com.example.jarvisv2.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shared Action model used by all screens to avoid duplicate/private
 * data class declarations and redeclaration errors.
 */
data class Action(
    val name: String,
    val icon: ImageVector,
    val command: String
)
