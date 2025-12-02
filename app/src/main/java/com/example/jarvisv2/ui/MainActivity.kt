package com.example.jarvisv2.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.jarvisv2.ui.navigation.AppRoot
import com.example.jarvisv2.ui.theme.JarvisV2Theme
import com.example.jarvisv2.viewmodel.MainViewModel

/**
 * Factory to create the MainViewModel with both Application and SavedStateHandle.
 */
class MainViewModelFactory(
    private val application: Application,
    owner: SavedStateRegistryOwner
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {

    // Use the custom factory to instantiate the ViewModel
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application, this)
    }

    // UPDATED: Handle multiple permissions (Audio + Notifications)
    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            // Only toggle voice service if audio is granted.
            if (audioGranted) {
                toggleVoiceService()
            }
        }

    // --- NEW: Companion object to track foreground state ---
    companion object {
        @Volatile
        var isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JarvisV2Theme {
                Surface {
                    AppRoot(
                        viewModel = viewModel,
                        onToggleVoiceService = { checkPermissionsAndToggleService() }
                    )
                }
            }
        }
    }

    // --- NEW: Lifecycle methods for Notification Logic ---
    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        clearMessageNotifications()
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
    }

    /**
     * Clears all dismissible notifications (Chat Messages).
     * Foreground Service notifications (Media, Monitor, Mic) are 'ongoing' and will persist.
     */
    private fun clearMessageNotifications() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // -------------------------------------------------------------
    // PERMISSIONS + VOICE SERVICE CONTROL
    // -------------------------------------------------------------
    private fun checkPermissionsAndToggleService() {
        val permissionsToRequest = mutableListOf<String>()

        // 1. Audio Permission (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
        }

        // 2. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            return
        }

        // Permission already granted
        toggleVoiceService()
    }

    private fun toggleVoiceService() {
        viewModel.toggleVoiceService()
    }
}