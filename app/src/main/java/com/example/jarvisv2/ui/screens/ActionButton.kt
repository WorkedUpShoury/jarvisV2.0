package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    icon: ImageVector,
    text: String,
    subtitle: String? = null,
    containerColor: Color = DarkSurface, // Default to dark navy
    elevation: Dp = 8.dp, // Default to standard elevation
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

            // ICON
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = DarkPrimary,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // MAIN LABEL
            Text(
                text = text,
                color = DarkOnSurface,
                fontSize = 13.sp
            )

            // OPTIONAL SUBTITLE
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

/**
 * Overload for ViewModel usage
 */
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
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
        text = text,
        subtitle = subtitle,
        containerColor = containerColor,
        elevation = elevation,
        onClick = { viewModel.sendButtonCommand(command) }
    )
}