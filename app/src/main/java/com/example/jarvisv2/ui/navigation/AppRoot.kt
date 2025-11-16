package com.example.jarvisv2.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jarvisv2.ui.components.ConnectionStatusIcon
import com.example.jarvisv2.ui.components.VoiceServiceToggleButton
import com.example.jarvisv2.ui.screens.AppsScreen
import com.example.jarvisv2.ui.screens.ChatScreen
import com.example.jarvisv2.ui.screens.PlayDialog
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

    val showPlayDialog by viewModel.showPlayDialog.collectAsState()

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
                    VoiceServiceToggleButton(
                        isRunningFlow = viewModel.isVoiceServiceRunning,
                        onToggle = onToggleVoiceService
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onPlayClicked() },
                containerColor = DarkPrimary
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Media")
            }
        },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val current = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.route,
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

        // Navigation Host
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

        // Play Dialog (Spotify / YouTube / Liked Songs)
        if (showPlayDialog) {
            PlayDialog(viewModel)
        }
    }
}
