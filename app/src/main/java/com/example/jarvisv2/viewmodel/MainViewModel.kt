package com.example.jarvisv2.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Parcelable
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
import com.example.jarvisv2.network.JarvisEventResponse
import com.example.jarvisv2.service.JarvisVoiceService
import com.example.jarvisv2.service.JarvisVoiceService.Companion.ServiceState
import com.example.jarvisv2.service.VoiceListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class MainViewModel(
    private val app: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val apiClient = JarvisApiClient(app.applicationContext)
    private val repository: ChatRepository

    private val prefs = app.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    // -----------------------------------------------------------------------------------------
    // INTERNAL STATES
    // -----------------------------------------------------------------------------------------

    private val _serverUrl = MutableStateFlow<String?>(null)
    private val _isDiscovering = MutableStateFlow(true)
    private val _lastCommandStatus = MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    private val _commandText = MutableStateFlow("")

    val chatHistory: StateFlow<List<ChatMessage>>

    private val _lastButtonCommand = MutableStateFlow<String?>(null)

    // -----------------------------------------------------------------------------------------
    // PUBLIC STATES (for UI)
    // -----------------------------------------------------------------------------------------

    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    val lastCommandStatus: StateFlow<CommandStatus> = _lastCommandStatus.asStateFlow()
    val commandText: StateFlow<String> = _commandText.asStateFlow()

    val isVoiceServiceRunning: StateFlow<ServiceState> =
        JarvisVoiceService.serviceState.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ServiceState.Stopped
        )

    val detailedVoiceState: StateFlow<VoiceListener.VoiceState> =
        JarvisVoiceService.detailedVoiceState.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            VoiceListener.VoiceState.Stopped
        )

    val suggestions: StateFlow<List<String>> = _commandText
        .map { text ->
            if (text.startsWith("/")) {
                val query = text.substring(1).lowercase()
                allCommands.filter { it.command.lowercase().contains(query) }
                    .map { it.command }
                    .take(5)
            } else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        val chatDao = AppDatabase.getDatabase(app).chatDao()
        repository = ChatRepository(chatDao)

        chatHistory = repository.allMessages
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        startServerDiscovery()
        observeVoiceServiceCommands()
        startEventPolling()
    }

    private fun startServerDiscovery() {
        viewModelScope.launch {
            _isDiscovering.value = true
            apiClient.discoverJarvisService()
                .catch { e ->
                    Log.e("MainViewModel", "Discovery failed: ${e.message}")
                    _isDiscovering.value = false
                }
                .collect { url ->
                    _serverUrl.value = url
                    _isDiscovering.value = false
                }
        }
    }

    private fun observeVoiceServiceCommands() {
        JarvisVoiceService.latestVoiceResult
            .filter { it.isNotEmpty() }
            .onEach { command ->
                Log.d("ViewModel", "Processing voice command: $command")
                addChatMessage(command, ChatSender.User)
                sendCommand(command)
                JarvisVoiceService.latestVoiceResult.value = ""
            }
            .launchIn(viewModelScope)
    }

    private fun startEventPolling() {
        viewModelScope.launch {
            serverUrl.collect { url ->
                if (url != null) {
                    var currentLastId = prefs.getInt("last_event_id", 0)

                    while (true) {
                        val result: Result<JarvisEventResponse> = apiClient.getEvents(url, currentLastId)
                        result.fold(
                            onSuccess = { response ->
                                val lastButtonCmd = _lastButtonCommand.value
                                for (event in response.events) {
                                    if (event.type == "speak") {
                                        // Don't log ephemeral button responses if we tracked them
                                        if (lastButtonCmd != null && event.text.equals(lastButtonCmd, ignoreCase = true)) {
                                            _lastButtonCommand.value = null
                                            Log.d("ViewModel", "Swallowed button response: ${event.text}")
                                        } else {
                                            addChatMessage(event.text, ChatSender.System)
                                        }
                                    }
                                }

                                if (response.last_id > currentLastId) {
                                    currentLastId = response.last_id
                                    prefs.edit().putInt("last_event_id", currentLastId).apply()
                                }
                            },
                            onFailure = { delay(5000) }
                        )
                        delay(1000)
                    }
                }
            }
        }
    }

    fun onCommandTextChanged(text: String) {
        _commandText.value = text
    }

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
            return
        }

        viewModelScope.launch {
            _lastCommandStatus.value = CommandStatus.Loading
            val result: Result<JarvisCommandResponse> = apiClient.sendCommand(url, command)
            result.fold(
                onSuccess = {
                    _lastCommandStatus.value = CommandStatus.Success
                },
                onFailure = {
                    _lastCommandStatus.value = CommandStatus.Error(it.message ?: "Unknown error")
                    addChatMessage("Error: ${it.message}", ChatSender.System)
                }
            )
        }
    }

    fun sendButtonCommand(command: String) {
        _lastButtonCommand.value = command
        sendCommand(command)
    }

    // --- UDP Command Logic ---
    fun sendUdpCommand(message: String) {
        // We need the base IP from the current server URL
        val fullUrl = _serverUrl.value ?: return

        viewModelScope.launch {
            try {
                // Example URL: "http://192.168.1.15:8765" -> "192.168.1.15"
                val hostIp = fullUrl
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .substringBefore(":")

                if (hostIp.isNotEmpty()) {
                    apiClient.sendUdpCommand(hostIp, message)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sending UDP command: ${e.message}")
            }
        }
    }

    fun toggleVoiceService() {
        val intent = Intent(app.applicationContext, JarvisVoiceService::class.java)
        if (isVoiceServiceRunning.value == ServiceState.Running) {
            app.applicationContext.stopService(intent)
        } else {
            app.applicationContext.startForegroundService(intent)
        }
    }

    private fun addChatMessage(message: String, sender: ChatSender) {
        viewModelScope.launch {
            val chat = ChatMessage(message = message, sender = sender)
            repository.insert(chat)
        }
    }

    // --- NEW: Clear & Delete functionality ---
    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteChatMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.delete(message)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// SUPPORTING TYPES
// ---------------------------------------------------------------------------------------------

sealed class CommandStatus {
    object Idle : CommandStatus()
    object Loading : CommandStatus()
    object Success : CommandStatus()
    data class Error(val message: String) : CommandStatus()
}

@Parcelize
enum class ChatSender : Parcelable {
    User, System
}