package com.example.jarvisv2.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisv2.data.allCommands
import com.example.jarvisv2.network.JarvisApiClient
import com.example.jarvisv2.service.JarvisVoiceService
import com.example.jarvisv2.service.JarvisVoiceService.Companion.ServiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val apiClient = JarvisApiClient(app.applicationContext)

    // -----------------------------------------------------------------------------------------
    // INTERNAL STATES
    // -----------------------------------------------------------------------------------------

    private val _serverUrl = MutableStateFlow<String?>(null)
    private val _isDiscovering = MutableStateFlow(true)
    private val _lastCommandStatus = MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    private val _commandText = MutableStateFlow("")
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())

    // Play dialog (new)
    private val _showPlayDialog = MutableStateFlow(false)
    val showPlayDialog: StateFlow<Boolean> = _showPlayDialog.asStateFlow()
    fun onPlayClicked() { _showPlayDialog.value = true }
    fun onPlayDialogDismiss() { _showPlayDialog.value = false }

    // -----------------------------------------------------------------------------------------
    // PUBLIC STATES (for UI)
    // -----------------------------------------------------------------------------------------

    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    val lastCommandStatus: StateFlow<CommandStatus> = _lastCommandStatus.asStateFlow()
    val commandText: StateFlow<String> = _commandText.asStateFlow()
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    /**
     * Lifecycle-aware wrapper for service state
     */
    val isVoiceServiceRunning: StateFlow<ServiceState> =
        JarvisVoiceService.serviceState.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ServiceState.Stopped
        )

    /**
     * Slash command suggestions.
     */
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
        startServerDiscovery()
        observeVoiceServiceCommands()
    }

    private fun startServerDiscovery() {
        viewModelScope.launch {
            _isDiscovering.value = true
            apiClient.discoverJarvisService().collect { url ->
                _serverUrl.value = url
                _isDiscovering.value = false
                addChatMessage("Jarvis server found at $url", ChatSender.System)
            }
        }
    }

    private fun observeVoiceServiceCommands() {
        viewModelScope.launch {
            JarvisVoiceService.latestVoiceResult.collect { command ->
                if (command.isNotEmpty()) {
                    addChatMessage(command, ChatSender.User)
                    sendCommand(command)
                    JarvisVoiceService.latestVoiceResult.value = ""
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

    fun sendCommand(command: String) {
        val url = _serverUrl.value
        if (url == null) {
            _lastCommandStatus.value = CommandStatus.Error("Server not found.")
            addChatMessage("Error: Server not found.", ChatSender.System)
            return
        }

        viewModelScope.launch {
            _lastCommandStatus.value = CommandStatus.Loading
            val result = apiClient.sendCommand(url, command)
            result.fold(
                onSuccess = {
                    _lastCommandStatus.value = CommandStatus.Success
                    addChatMessage("Command sent: $command", ChatSender.System)
                },
                onFailure = {
                    _lastCommandStatus.value = CommandStatus.Error(it.message ?: "Unknown error")
                    addChatMessage("Error: ${it.message}", ChatSender.System)
                }
            )
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
        val chat = ChatMessage(message = message, sender = sender)
        _chatHistory.value = _chat_history_plus(chat)
    }

    // small helper to clone list (avoids some concurrency edge cases)
    private fun _chat_history_plus(chat: ChatMessage): List<ChatMessage> =
        synchronized(this) { _chatHistory.value + chat }
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

enum class ChatSender {
    User, System
}

data class ChatMessage(
    val message: String,
    val sender: ChatSender,
    val timestamp: Long = System.currentTimeMillis()
)
