package com.example.jarvisv2.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shared Action model used by all screens.
 * Updated to support both Vector Icons (system) and Drawable Resources (custom brand logos).
 */
data class Action(
    val name: String,
    val icon: ImageVector? = null, // Defaults to null so we can use iconDrawable instead
    val command: String,
    val iconDrawable: Int? = null  // New field for R.drawable.ic_your_icon
)