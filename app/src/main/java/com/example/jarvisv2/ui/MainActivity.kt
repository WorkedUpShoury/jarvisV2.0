package com.example.jarvisv2.ui

import android.Manifest
import android.app.Application
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

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                toggleVoiceService()
            }
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

    // -------------------------------------------------------------
    // PERMISSIONS + VOICE SERVICE CONTROL
    // -------------------------------------------------------------
    private fun checkPermissionsAndToggleService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = Manifest.permission.RECORD_AUDIO

            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                audioPermissionLauncher.launch(permission)
                return
            }
        }

        // Permission already granted
        toggleVoiceService()
    }

    private fun toggleVoiceService() {
        viewModel.toggleVoiceService()
    }
}