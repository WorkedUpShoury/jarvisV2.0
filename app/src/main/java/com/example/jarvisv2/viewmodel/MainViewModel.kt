package com.example.jarvisv2.viewmodel

import android.app.Application
import android.content.Intent
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
import com.example.jarvisv2.service.JarvisNotificationService // <--- NEW IMPORT
import com.example.jarvisv2.service.JarvisVoiceService
import com.example.jarvisv2.service.JarvisVoiceService.Companion.ServiceState
import com.example.jarvisv2.service.VoiceListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

class MainViewModel(
    private val app: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val apiClient = JarvisApiClient(app.applicationContext)
    private val repository: ChatRepository

    // --- STATES ---
    private val _serverUrl = MutableStateFlow<String?>(null)
    private val _isDiscovering = MutableStateFlow(true)
    private val _lastCommandStatus = MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    private val _commandText = MutableStateFlow("")
    private val _lastButtonCommand = MutableStateFlow<String?>(null)

    private val _volumeLevel = MutableStateFlow(5f)
    private val _brightnessLevel = MutableStateFlow(5f)

    val mediaState: StateFlow<MediaStateResponse?> = JarvisMediaService.sharedMediaState.asStateFlow()

    private var consecutiveFailures = 0
    private var discoveryJob: Job? = null

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
        val chatDao = AppDatabase.getDatabase(app).chatDao()
        repository = ChatRepository(chatDao)
        chatHistory = repository.allMessages.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        startServerDiscovery()
        observeVoiceServiceCommands()
        startHistoryPolling()
    }

    private fun startServerDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _isDiscovering.value = true
            _serverUrl.value = null

            apiClient.discoverJarvisService()
                .catch { e ->
                    _isDiscovering.value = false
                    delay(3000)
                    startServerDiscovery()
                }
                .collect { url ->
                    _serverUrl.value = url
                    _isDiscovering.value = false
                    consecutiveFailures = 0

                    // 1. Start Media Service
                    JarvisMediaService.serverUrl = url
                    val mediaIntent = Intent(app.applicationContext, JarvisMediaService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        app.applicationContext.startForegroundService(mediaIntent)
                    } else {
                        app.applicationContext.startService(mediaIntent)
                    }

                    // 2. Start Notification Service (NEW)
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

    // --- HISTORY POLLING (Source of Truth) ---
    private fun startHistoryPolling() {
        viewModelScope.launch {
            serverUrl.collect { url ->
                if (url != null) {
                    // FIX: Clear local DB on new connection to prevent duplicates
                    repository.clearAll()

                    var processedCount = 0

                    while (true) {
                        if (_serverUrl.value == url) {
                            apiClient.getChatHistory(url).fold(
                                onSuccess = { history ->
                                    consecutiveFailures = 0

                                    // If history on server shrunk (e.g. file cleared), reset local counter
                                    if (history.size < processedCount) {
                                        processedCount = 0
                                        // Optional: Clear again if server history was wiped
                                        repository.clearAll()
                                    }

                                    // Process NEW items
                                    if (history.size > processedCount) {
                                        val newItems = history.subList(processedCount, history.size)
                                        for (item in newItems) {
                                            // Convert server role to UI sender
                                            val sender = if (item.role == "user") ChatSender.User else ChatSender.System
                                            val message = item.parts.text

                                            // Insert into local DB for display
                                            repository.insert(ChatMessage(message = message, sender = sender))
                                        }
                                        processedCount = history.size
                                    }
                                },
                                onFailure = {
                                    consecutiveFailures++
                                    if (consecutiveFailures > 3) startServerDiscovery()
                                    delay(3000)
                                }
                            )
                        }
                        delay(1500) // Poll every 1.5s
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

    fun sendCurrentCommand() {
        val command = _commandText.value.trim()
        if (command.isEmpty()) return

        // We do NOT insert the message locally immediately.
        // We send it to server -> Server writes to file -> We poll file -> UI updates.
        // This guarantees perfect sync.
        sendCommand(command)
        _commandText.value = ""
    }

    private fun sendCommand(command: String) {
        val url = _serverUrl.value
        if (url == null) {
            // If disconnected, show local error
            viewModelScope.launch {
                repository.insert(ChatMessage(message = "Error: Server not found.", sender = ChatSender.System))
            }
            startServerDiscovery()
            return
        }

        viewModelScope.launch {
            _lastCommandStatus.value = CommandStatus.Loading
            apiClient.sendCommand(url, command).fold(
                onSuccess = {
                    _lastCommandStatus.value = CommandStatus.Success
                },
                onFailure = {
                    _lastCommandStatus.value = CommandStatus.Error(it.message ?: "Unknown error")
                }
            )
        }
    }

    fun sendButtonCommand(command: String) {
        _lastButtonCommand.value = command
        sendCommand(command)
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
                // Voice commands are sent to server, server writes to history
                sendCommand(command)
                JarvisVoiceService.latestVoiceResult.value = ""
            }
            .launchIn(viewModelScope)
    }

    fun clearChatHistory() = viewModelScope.launch { repository.clearAll() }
    fun deleteChatMessage(message: ChatMessage) = viewModelScope.launch { repository.delete(message) }
}

sealed class CommandStatus {
    object Idle : CommandStatus()
    object Loading : CommandStatus()
    object Success : CommandStatus()
    data class Error(val message: String) : CommandStatus()
}

@Parcelize
enum class ChatSender : Parcelable { User, System }