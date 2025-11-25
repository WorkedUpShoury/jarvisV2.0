package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
    elevation: Dp = 6.dp, // Slightly reduced elevation for cleaner look
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(95.dp), // Fixed height instead of AspectRatio for better fitting
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp), // Reduced padding slightly
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- ICON LOGIC ---
            if (iconDrawable != null) {
                Icon(
                    painter = painterResource(id = iconDrawable),
                    contentDescription = text,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp) // Adjusted size
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = DarkPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                color = DarkOnSurface,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = DarkOnSurface.copy(alpha = 0.7f),
                    maxLines = 1
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
    elevation: Dp = 6.dp,
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