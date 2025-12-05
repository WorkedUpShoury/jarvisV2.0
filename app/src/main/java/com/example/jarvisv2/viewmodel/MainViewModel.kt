package com.example.jarvisv2.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.jarvisv2.data.AppDatabase
import com.example.jarvisv2.data.ChatMessage
import com.example.jarvisv2.data.ChatRepository
import com.example.jarvisv2.data.allCommands
import com.example.jarvisv2.network.JarvisApiClient
import com.example.jarvisv2.network.MediaStateResponse
import com.example.jarvisv2.service.JarvisMediaService
import com.example.jarvisv2.service.JarvisNotificationService
import com.example.jarvisv2.service.JarvisVoiceService
import com.example.jarvisv2.service.JarvisVoiceService.Companion.ServiceState
import com.example.jarvisv2.service.VoiceListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.example.jarvisv2.data.MediaSearch
import com.example.jarvisv2.data.MediaSearchRepository
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest

class MainViewModel(
    private val app: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val apiClient = JarvisApiClient(app.applicationContext)
    private val chatRepository: ChatRepository
    private val mediaSearchRepository: MediaSearchRepository

    // --- STATES ---
    private val _serverUrl = MutableStateFlow<String?>(null)
    private val _isDiscovering = MutableStateFlow(true)
    private val _lastCommandStatus = MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    private val _commandText = MutableStateFlow("")
    private val _lastButtonCommand = MutableStateFlow<String?>(null)

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _volumeLevel = MutableStateFlow(5f)
    private val _brightnessLevel = MutableStateFlow(5f)

    val mediaState: StateFlow<MediaStateResponse?> = JarvisMediaService.sharedMediaState.asStateFlow()

    val recentMediaSearches: StateFlow<List<MediaSearch>>
    val mostSearchedQuery: StateFlow<MediaSearch?>

    private var consecutiveFailures = 0
    private var discoveryJob: Job? = null

    // --- CONNECTIVITY MONITORING ---
    private val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // When WiFi connects, restart discovery immediately
            startServerDiscovery()
        }
        override fun onLost(network: Network) {
            _serverUrl.value = null
            _isDiscovering.value = true
        }
    }

    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    val commandText: StateFlow<String> = _commandText.asStateFlow()
    val chatHistory: StateFlow<List<ChatMessage>>
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()
    val brightnessLevel: StateFlow<Float> = _brightnessLevel.asStateFlow()

    val isVoiceServiceRunning: StateFlow<ServiceState> =
        JarvisVoiceService.serviceState.stateIn(viewModelScope, SharingStarted.Eagerly, ServiceState.Stopped)

    val detailedVoiceState: StateFlow<VoiceListener.VoiceState> =
        JarvisVoiceService.detailedVoiceState.stateIn(viewModelScope, SharingStarted.Eagerly, VoiceListener.VoiceState.Stopped)

    val suggestions: StateFlow<List<String>> = _commandText.map { text ->
        if (text.startsWith("/")) {
            val query = text.substring(1).lowercase()
            allCommands.filter { it.command.lowercase().contains(query) }.map { it.command }.take(5)
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        val database = AppDatabase.getDatabase(app)
        chatRepository = ChatRepository(database.chatDao())
        mediaSearchRepository = MediaSearchRepository(database.mediaSearchDao())

        chatHistory = chatRepository.allMessages.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        recentMediaSearches = mediaSearchRepository.recentSearches.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        mostSearchedQuery = mediaSearchRepository.mostSearchedQuery.stateIn(viewModelScope, SharingStarted.Lazily, null)

        // Register Network Callback
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startServerDiscovery()
        observeVoiceServiceCommands()
        startHistoryPolling()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startServerDiscovery() {
        // Cancel any existing job to ensure a fresh start
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _isDiscovering.value = true
            _serverUrl.value = null

            apiClient.discoverJarvisService()
                .catch { e ->
                    // On error, wait and try again
                    _isDiscovering.value = false
                    delay(3000)
                    if (serverUrl.value == null) {
                        startServerDiscovery()
                    }
                }
                .collect { url ->
                    _serverUrl.value = url
                    _isDiscovering.value = false
                    consecutiveFailures = 0

                    JarvisMediaService.serverUrl = url
                    val mediaIntent = Intent(app.applicationContext, JarvisMediaService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        app.applicationContext.startForegroundService(mediaIntent)
                    } else {
                        app.applicationContext.startService(mediaIntent)
                    }

                    JarvisNotificationService.serverUrl = url
                    val notifIntent = Intent(app.applicationContext, JarvisNotificationService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        app.applicationContext.startForegroundService(notifIntent)
                    } else {
                        app.applicationContext.startService(notifIntent)
                    }

                    fetchSystemLevels()
                }
        }
    }

    private fun startHistoryPolling() {
        viewModelScope.launch {
            serverUrl.collect { url ->
                if (url != null) {
                    chatRepository.clearAll()
                    var processedCount = 0
                    while (true) {
                        if (_serverUrl.value == url) {
                            apiClient.getChatHistory(url).fold(
                                onSuccess = { history ->
                                    consecutiveFailures = 0
                                    if (history.size < processedCount) {
                                        processedCount = 0
                                        chatRepository.clearAll()
                                    }
                                    if (history.size > processedCount) {
                                        val newItems = history.subList(processedCount, history.size)
                                        for (item in newItems) {
                                            val sender = if (item.role == "user") ChatSender.User else ChatSender.System
                                            val message = item.parts.text
                                            chatRepository.insert(ChatMessage(message = message, sender = sender))
                                        }
                                        processedCount = history.size
                                    }
                                },
                                onFailure = {
                                    consecutiveFailures++
                                    // If failing repeatedly, trigger a fresh discovery
                                    if (consecutiveFailures > 3) {
                                        startServerDiscovery()
                                    }
                                    delay(3000)
                                }
                            )
                        }
                        delay(1500)
                    }
                }
            }
        }
    }

    fun fetchSystemLevels() {
        val url = _serverUrl.value ?: return
        viewModelScope.launch {
            apiClient.getSystemLevels(url).onSuccess { levels ->
                _volumeLevel.value = levels.volume / 10f
                _brightnessLevel.value = levels.brightness / 10f
            }
        }
    }

    fun updateVolumeState(newVal: Float) { _volumeLevel.value = newVal }
    fun updateBrightnessState(newVal: Float) { _brightnessLevel.value = newVal }

    fun onCommandTextChanged(text: String) { _commandText.value = text }

    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    // --- CONVERSATION MODE SUPPORT ---
    fun sendCurrentCommand(isConversation: Boolean = false) {
        val command = _commandText.value.trim()
        val imageUri = _selectedImageUri.value

        if (command.isEmpty() && imageUri == null) return

        if (imageUri != null) {
            val finalCommand = if (command.isEmpty()) "Analyze this image" else command
            sendImageCommand(finalCommand, imageUri)
        } else {
            sendCommand(command, isConversation)
        }

        _commandText.value = ""
        _selectedImageUri.value = null
    }

    private fun sendCommand(command: String, isConversation: Boolean = false) {
        val url = _serverUrl.value
        if (url == null) {
            viewModelScope.launch {
                chatRepository.insert(ChatMessage(message = "Error: Server not found.", sender = ChatSender.System))
            }
            startServerDiscovery()
            return
        }

        viewModelScope.launch {
            _lastCommandStatus.value = CommandStatus.Loading
            apiClient.sendCommand(url, command, isConversation).fold(
                onSuccess = { _lastCommandStatus.value = CommandStatus.Success },
                onFailure = { _lastCommandStatus.value = CommandStatus.Error(it.message ?: "Unknown error") }
            )
        }
    }

    private fun sendImageCommand(command: String, uri: Uri) {
        val url = _serverUrl.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _lastCommandStatus.value = CommandStatus.Loading
            try {
                val bytes = getCompressedImageBytes(uri)
                if (bytes != null) {
                    apiClient.sendCommandWithImage(url, command, bytes).fold(
                        onSuccess = { _lastCommandStatus.value = CommandStatus.Success },
                        onFailure = { _lastCommandStatus.value = CommandStatus.Error(it.message ?: "Upload failed") }
                    )
                } else {
                    _lastCommandStatus.value = CommandStatus.Error("Failed to process image")
                }
            } catch (e: Exception) {
                _lastCommandStatus.value = CommandStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getCompressedImageBytes(uri: Uri): ByteArray? {
        return try {
            val inputStream = app.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sendButtonCommand(command: String) {
        _lastButtonCommand.value = command
        sendCommand(command) // Standard mode
    }

    fun sendUdpCommand(message: String) {
        val fullUrl = _serverUrl.value ?: return
        viewModelScope.launch {
            try {
                val hostIp = fullUrl.removePrefix("http://").removePrefix("https://").substringBefore(":")
                if (hostIp.isNotEmpty()) apiClient.sendUdpCommand(hostIp, message)
            } catch (e: Exception) { }
        }
    }

    fun toggleVoiceService() {
        val intent = Intent(app.applicationContext, JarvisVoiceService::class.java)
        if (isVoiceServiceRunning.value == ServiceState.Running) {
            app.applicationContext.stopService(intent)
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.applicationContext.startForegroundService(intent)
            } else {
                app.applicationContext.startService(intent)
            }
        }
    }

    private fun observeVoiceServiceCommands() {
        JarvisVoiceService.latestVoiceResult
            .filter { it.isNotEmpty() }
            .onEach { command ->
                sendCommand(command)
                JarvisVoiceService.latestVoiceResult.value = ""
            }
            .launchIn(viewModelScope)
    }

    fun sendChatDeleteCommand(messageText: String) {
        val hash = generateStableHash(messageText)
        val command = "delete chat message $hash"
        sendButtonCommand(command)
    }

    private fun generateStableHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.slice(0..3).joinToString("") { "%02x".format(it) }
    }

    fun saveMediaSearch(query: String, source: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSearches = mediaSearchRepository.recentSearches.first()
            val existing = currentSearches.find { it.query.lowercase() == query.lowercase() }

            val newSearch = if (existing != null) {
                existing.copy(
                    count = existing.count + 1,
                    lastUsed = System.currentTimeMillis(),
                    source = source
                )
            } else {
                MediaSearch(query = query, source = source, count = 1, lastUsed = System.currentTimeMillis())
            }

            mediaSearchRepository.getDao().insertOrUpdate(newSearch)
        }
    }

    fun playMediaSearch(query: String, source: String) {
        val command = if (source == "spotify") "play $query on spotify" else "play $query"
        sendButtonCommand(command)
    }

    fun clearMediaSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            mediaSearchRepository.clearAll()
        }
    }
}

sealed class CommandStatus {
    object Idle : CommandStatus()
    object Loading : CommandStatus()
    object Success : CommandStatus()
    data class Error(val message: String) : CommandStatus()
}

@Parcelize
enum class ChatSender : Parcelable { User, System }