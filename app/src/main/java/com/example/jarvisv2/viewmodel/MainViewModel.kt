package com.example.jarvisv2.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.jarvisv2.data.AppDatabase
import com.example.jarvisv2.data.ChatMessage
import com.example.jarvisv2.data.ChatRepository
import com.example.jarvisv2.data.allCommands
import com.example.jarvisv2.network.JarvisApiClient
import com.example.jarvisv2.network.JarvisCommandResponse
import com.example.jarvisv2.network.MediaStateResponse
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
    // Note: No SharedPrefs for event ID to prevent sync issues

    // --- STATES ---
    private val _serverUrl = MutableStateFlow<String?>(null)
    private val _isDiscovering = MutableStateFlow(true)
    private val _lastCommandStatus = MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    private val _commandText = MutableStateFlow("")
    private val _lastButtonCommand = MutableStateFlow<String?>(null)

    // System Levels
    private val _volumeLevel = MutableStateFlow(5f)
    private val _brightnessLevel = MutableStateFlow(5f)

    // Media State: Observe the Service's shared state (Single Source of Truth)
    // This ensures UI matches the background notification
    val mediaState: StateFlow<MediaStateResponse?> = JarvisVoiceService.sharedMediaState.asStateFlow()

    // Connection Health
    private var consecutiveFailures = 0
    private var discoveryJob: Job? = null

    // Public Flows
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
        startEventPolling()
    }

    // -----------------------------------------------------------------------------------------
    // SERVER DISCOVERY & CONNECTION
    // -----------------------------------------------------------------------------------------
    private fun startServerDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _isDiscovering.value = true
            _serverUrl.value = null

            apiClient.discoverJarvisService()
                .catch { e ->
                    Log.e("MainViewModel", "Discovery failed: ${e.message}")
                    _isDiscovering.value = false
                    delay(3000)
                    startServerDiscovery()
                }
                .collect { url ->
                    _serverUrl.value = url
                    _isDiscovering.value = false
                    consecutiveFailures = 0

                    // --- SYNC WITH SERVICE ---
                    // Pass URL to Service so it can poll media in background
                    JarvisVoiceService.serverUrl = url

                    // Ensure service is running for notifications
                    if (JarvisVoiceService.serviceState.value == ServiceState.Running) {
                        val intent = Intent(app.applicationContext, JarvisVoiceService::class.java)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            app.applicationContext.startForegroundService(intent)
                        } else {
                            app.applicationContext.startService(intent)
                        }
                    }

                    fetchSystemLevels()
                }
        }
    }

    private fun reportConnectionFailure() {
        consecutiveFailures++
        if (consecutiveFailures >= 3) {
            consecutiveFailures = 0
            startServerDiscovery()
        }
    }

    // -----------------------------------------------------------------------------------------
    // SYSTEM LEVELS
    // -----------------------------------------------------------------------------------------
    fun fetchSystemLevels() {
        val url = _serverUrl.value ?: return
        viewModelScope.launch {
            apiClient.getSystemLevels(url).fold(
                onSuccess = { levels ->
                    _volumeLevel.value = levels.volume / 10f
                    _brightnessLevel.value = levels.brightness / 10f
                },
                onFailure = { reportConnectionFailure() }
            )
        }
    }

    fun updateVolumeState(newVal: Float) { _volumeLevel.value = newVal }
    fun updateBrightnessState(newVal: Float) { _brightnessLevel.value = newVal }

    // -----------------------------------------------------------------------------------------
    // COMMANDS
    // -----------------------------------------------------------------------------------------
    fun onCommandTextChanged(text: String) { _commandText.value = text }

    fun sendCurrentCommand() {
        val command = _commandText.value.trim()
        if (command.isEmpty()) return
        val commandToSend = if (command.startsWith("/")) command.substring(1) else command
        addChatMessage(commandToSend, ChatSender.User)
        sendCommand(commandToSend)
        _commandText.value = ""
    }

    private fun sendCommand(command: String) {
        val url = _serverUrl.value
        if (url == null) {
            addChatMessage("Error: Server not found.", ChatSender.System)
            startServerDiscovery()
            return
        }

        viewModelScope.launch {
            _lastCommandStatus.value = CommandStatus.Loading
            apiClient.sendCommand(url, command).fold(
                onSuccess = {
                    _lastCommandStatus.value = CommandStatus.Success
                    consecutiveFailures = 0
                },
                onFailure = {
                    _lastCommandStatus.value = CommandStatus.Error(it.message ?: "Unknown error")
                    addChatMessage("Error: ${it.message}", ChatSender.System)
                    reportConnectionFailure()
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
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sending UDP command: ${e.message}")
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // EVENT POLLING
    // -----------------------------------------------------------------------------------------
    private fun startEventPolling() {
        viewModelScope.launch {
            serverUrl.collect { url ->
                if (url != null) {
                    var currentLastId = 0
                    while (true) {
                        if (_serverUrl.value == url) {
                            apiClient.getEvents(url, currentLastId).fold(
                                onSuccess = { response ->
                                    consecutiveFailures = 0
                                    if (response.last_id < currentLastId) {
                                        currentLastId = 0
                                    } else {
                                        val lastButtonCmd = _lastButtonCommand.value
                                        for (event in response.events) {
                                            if (event.type == "chat") {
                                                addChatMessage(event.text, ChatSender.System)
                                            }
                                        }
                                        if (response.last_id > currentLastId) {
                                            currentLastId = response.last_id
                                        }
                                    }
                                },
                                onFailure = {
                                    reportConnectionFailure()
                                    delay(3000)
                                }
                            )
                        }
                        delay(1000)
                    }
                }
            }
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
                addChatMessage(command, ChatSender.User)
                sendCommand(command)
                JarvisVoiceService.latestVoiceResult.value = ""
            }
            .launchIn(viewModelScope)
    }

    private fun addChatMessage(message: String, sender: ChatSender) {
        viewModelScope.launch {
            repository.insert(ChatMessage(message = message, sender = sender))
        }
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