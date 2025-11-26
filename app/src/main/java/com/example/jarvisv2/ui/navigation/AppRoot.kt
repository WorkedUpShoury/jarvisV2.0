package com.example.jarvisv2.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeOff
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
import com.example.jarvisv2.ui.screens.MediaScreen
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
        BottomNavItem.Media,
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
            val hideFab = currentRoute == BottomNavItem.Chat.route

            AnimatedVisibility(
                visible = !hideFab,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    AnimatedVisibility(visible = isFabExpanded) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.sendButtonCommand("resume playback")
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.PlayArrow, "Play")
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.sendButtonCommand("next track")
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.SkipNext, "Next")
                            }

                            // --- NEW FULLSCREEN BUTTON ---
                            SmallFloatingActionButton(
                                onClick = {
                                    // Sends "press f" command specifically as requested
                                    viewModel.sendButtonCommand("press f")
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.Fullscreen, "Fullscreen (f)")
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.sendButtonCommand("mute volume")
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.VolumeOff, "Mute")
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    navController.navigate(BottomNavItem.Media.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    isFabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.MusicNote, "Music")
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = DarkPrimary
                    ) {
                        Crossfade(targetState = isFabExpanded, label = "FabIcon") {
                            if (it) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = "Media Menu")
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
                SystemScreen(viewModel = viewModel)
            }
            composable(BottomNavItem.Media.route) {
                MediaScreen(viewModel = viewModel)
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