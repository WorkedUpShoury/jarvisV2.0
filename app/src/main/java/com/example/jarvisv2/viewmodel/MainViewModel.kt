package com.example.jarvisv2.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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

    // -----------------------------------------------------------------------------------------
    // INTERNAL STATES
    // -----------------------------------------------------------------------------------------

    private val _serverUrl = MutableStateFlow<String?>(null)
    private val _isDiscovering = MutableStateFlow(true)
    private val _lastCommandStatus = MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    private val _commandText = MutableStateFlow("")

    val chatHistory: StateFlow<List<ChatMessage>> =
        savedStateHandle.getStateFlow("chatHistory", emptyList())

    // --- FIX 2: Persist lastEventId in the SavedStateHandle ---
    private val lastEventId: StateFlow<Int> =
        savedStateHandle.getStateFlow("lastEventId", 0)

    // This holds the last command sent from a button, so we can ignore its spoken response.
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
            SharingStarted.Eagerly, // <-- THIS IS THE FIX
            ServiceState.Stopped
        )

    val detailedVoiceState: StateFlow<VoiceListener.VoiceState> =
        JarvisVoiceService.detailedVoiceState.stateIn(
            viewModelScope,
            SharingStarted.Eagerly, // <-- THIS IS THE FIX
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
        startServerDiscovery()
        observeVoiceServiceCommands()
        startEventPolling()
    }

    private fun startServerDiscovery() {
        viewModelScope.launch {
            _isDiscovering.value = true
            apiClient.discoverJarvisService().collect { url ->
                _serverUrl.value = url
                _isDiscovering.value = false

                // --- FIX 1: This block is removed ---
                // This was the source of the race condition that wiped your chat history.
                // The connection icon in the top bar is enough feedback.
                /*
                if (chatHistory.value.none { it.message.startsWith("Jarvis server found") }) {
                    addChatMessage("Jarvis server found at $url", ChatSender.System)
                }
                */
            }
        }
    }

    private fun observeVoiceServiceCommands() {
        // Use a more robust flow chain
        JarvisVoiceService.latestVoiceResult
            .filter { it.isNotEmpty() } // 1. Only let non-empty commands pass
            .onEach { command ->
                // 2. This code now only runs for valid commands
                Log.d("ViewModel", "Processing voice command: $command")
                addChatMessage(command, ChatSender.User)
                sendCommand(command)

                // 3. Clear the command *after* processing
                JarvisVoiceService.latestVoiceResult.value = ""
            }
            .launchIn(viewModelScope) // Launch this collector in the ViewModel's scope
    }

    private fun startEventPolling() {
        viewModelScope.launch {
            serverUrl.collect { url ->
                if (url != null) {
                    while (true) {
                        // --- FIX 2: Use the persisted lastEventId.value ---
                        val result: Result<JarvisEventResponse> = apiClient.getEvents(url, lastEventId.value)
                        result.fold(
                            onSuccess = { response ->
                                val lastButtonCmd = _lastButtonCommand.value
                                for (event in response.events) {
                                    if (event.type == "speak") {
                                        // Check if this "speak" event matches the last button command
                                        if (lastButtonCmd != null && event.text.equals(lastButtonCmd, ignoreCase = true)) {
                                            // It matches, so "swallow" it and don't add to chat
                                            _lastButtonCommand.value = null // Clear the command
                                            Log.d("ViewModel", "Swallowed button response: ${event.text}")
                                        } else {
                                            // Not a button response, so add it to the chat
                                            addChatMessage(event.text, ChatSender.System)
                                        }
                                    }
                                }
                                // --- FIX 2: Save the new lastEventId to the SavedStateHandle ---
                                savedStateHandle["lastEventId"] = response.last_id
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

        // 1. Add user's typed message to chat
        addChatMessage(commandToSend, ChatSender.User)
        // 2. Send command to server
        sendCommand(commandToSend)
        // 3. Clear text box
        _commandText.value = ""
    }

    /**
     * Sends a command that originated from chat or voice.
     * The server's response WILL be added to the chat.
     */
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

    /**
     * Sends a command from a button.
     * The server's response will be "swallowed" and NOT added to the chat.
     */
    fun sendButtonCommand(command: String) {
        // Set the command to be "swallowed"
        _lastButtonCommand.value = command

        // Send the command normally
        sendCommand(command)
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
        val currentHistory = chatHistory.value
        savedStateHandle["chatHistory"] = currentHistory + chat
    }
}

// ---------------------------------------------------------------------------------------------
// SUPPORTING TYPES (Parcelable)
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

@Parcelize
data class ChatMessage(
    val message: String,
    val sender: ChatSender,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable