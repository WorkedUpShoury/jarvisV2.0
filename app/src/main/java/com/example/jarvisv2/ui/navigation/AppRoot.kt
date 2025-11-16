package com.example.jarvisv2.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jarvisv2.ui.components.ConnectionStatusIcon
import com.example.jarvisv2.ui.components.VoiceStatusIcon
import com.example.jarvisv2.ui.screens.AppsScreen
import com.example.jarvisv2.ui.screens.ChatScreen
import com.example.jarvisv2.ui.screens.SystemScreen
import com.example.jarvisv2.ui.screens.WebScreen
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    viewModel: MainViewModel,
    onToggleVoiceService: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.System,
        BottomNavItem.Apps,
        BottomNavItem.Web,
        BottomNavItem.Chat
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jarvis V2 Control") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                ),
                navigationIcon = {
                    ConnectionStatusIcon(
                        serverUrlFlow = viewModel.serverUrl,
                        isDiscoveringFlow = viewModel.isDiscovering
                    )
                },
                actions = {
                    VoiceStatusIcon(
                        detailedStateFlow = viewModel.detailedVoiceState,
                        onToggle = onToggleVoiceService
                    )
                }
            )
        },
        floatingActionButton = {
            // Hide all FABs on the Chat screen
            AnimatedVisibility(
                visible = currentRoute != BottomNavItem.Chat.route,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Expanding sub-buttons
                    AnimatedVisibility(visible = isFabExpanded) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.sendButtonCommand("next track")
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.SkipNext, "Next Track")
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.sendButtonCommand("previous track")
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.SkipPrevious, "Previous Track")
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.sendButtonCommand("pause playback")
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.Pause, "Pause Playback")
                            }
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = DarkPrimary
                    ) {
                        Crossfade(targetState = isFabExpanded, label = "FabIcon") {
                            if (it) {
                                Icon(Icons.Default.Close, contentDescription = "Close Media")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Media")
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                restoreState = true
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.System.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.System.route) {
                SystemScreen(viewModel)
            }
            composable(BottomNavItem.Apps.route) {
                AppsScreen(viewModel)
            }
            composable(BottomNavItem.Web.route) {
                WebScreen(viewModel)
            }
            composable(BottomNavItem.Chat.route) {
                ChatScreen(viewModel)
            }
        }
    }
}