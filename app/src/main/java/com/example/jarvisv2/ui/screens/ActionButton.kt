package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconDrawable: Int? = null,
    text: String,
    subtitle: String? = null,
    containerColor: Color = DarkSurface,
    elevation: Dp = 8.dp,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(110.dp)
            .aspectRatio(1f),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- ICON LOGIC ---
            if (iconDrawable != null) {
                // 1. Priority: Custom Drawable (Official Brand Icon)
                Icon(
                    painter = painterResource(id = iconDrawable),
                    contentDescription = text,
                    tint = Color.Unspecified, // Keeps original colors (if any)
                    modifier = Modifier.size(34.dp)
                )
            } else if (icon != null) {
                // 2. Fallback: Standard Vector Icon
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = DarkPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
                color = DarkOnSurface,
                fontSize = 13.sp
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = DarkOnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconDrawable: Int? = null,
    text: String,
    subtitle: String? = null,
    containerColor: Color = DarkSurface,
    elevation: Dp = 8.dp,
    viewModel: MainViewModel,
    command: String
) {
    ActionButton(
        modifier = modifier,
        icon = icon,
        iconDrawable = iconDrawable,
        text = text,
        subtitle = subtitle,
        containerColor = containerColor,
        elevation = elevation,
        onClick = { viewModel.sendButtonCommand(command) }
    )
}