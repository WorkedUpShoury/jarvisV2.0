package com.example.jarvisv2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.jarvisv2.R
import com.example.jarvisv2.network.JarvisApiClient
import com.example.jarvisv2.network.MediaStateResponse
import com.example.jarvisv2.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JarvisVoiceService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var voiceListener: VoiceListener
    private lateinit var apiClient: JarvisApiClient

    companion object {
        // Notification Actions
        const val ACTION_PLAY = "com.example.jarvisv2.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.jarvisv2.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.jarvisv2.ACTION_NEXT"
        const val ACTION_PREV = "com.example.jarvisv2.ACTION_PREV"
        const val ACTION_STOP = "com.example.jarvisv2.ACTION_STOP"

        enum class ServiceState { Running, Stopped, Error }

        var serviceState = MutableStateFlow(ServiceState.Stopped)
        var latestVoiceResult = MutableStateFlow("")
        var detailedVoiceState = MutableStateFlow<VoiceListener.VoiceState>(VoiceListener.VoiceState.Stopped)

        // Shared Media State (optional use by UI)
        var sharedMediaState = MutableStateFlow<MediaStateResponse?>(null)
        var serverUrl: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        apiClient = JarvisApiClient(this)
        voiceListener = VoiceListener(this)
        serviceState.value = ServiceState.Running

        voiceListener.voiceState.onEach { state ->
            detailedVoiceState.value = state
            if (state is VoiceListener.VoiceState.Result) {
                val text = state.text.lowercase().trim()
                if (text.contains("jarvis")) {
                    val command = text.replace("jarvis", "").trim()
                    if (command.isNotEmpty()) latestVoiceResult.value = command
                }
            }
        }.launchIn(serviceScope)

        voiceListener.startListening()

        // Start Background Media Polling for Notification
        startMediaPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle Media Button Clicks from Notification
        when (intent?.action) {
            ACTION_PLAY -> sendMediaCommand("resume playback")
            ACTION_PAUSE -> sendMediaCommand("pause playback")
            ACTION_NEXT -> sendMediaCommand("next track")
            ACTION_PREV -> sendMediaCommand("previous track")
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        updateNotification(sharedMediaState.value)
        return START_STICKY
    }

    private fun sendMediaCommand(cmd: String) {
        val url = serverUrl ?: return
        serviceScope.launch(Dispatchers.IO) {
            apiClient.sendCommand(url, cmd)
            delay(500)
            fetchMediaInfo(url) // Refresh immediately
        }
    }

    private fun startMediaPolling() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val url = serverUrl
                if (url != null) {
                    fetchMediaInfo(url)
                }
                delay(2000) // Poll every 2s
            }
        }
    }

    private suspend fun fetchMediaInfo(url: String) {
        apiClient.getMediaState(url).onSuccess { info ->
            if (sharedMediaState.value != info) {
                sharedMediaState.value = info
                withContext(Dispatchers.Main) {
                    updateNotification(info)
                }
            }
        }
    }

    private fun updateNotification(mediaInfo: MediaStateResponse?) {
        val channelId = "JarvisVoiceServiceChannel"
        createNotificationChannel(channelId)

        // Clicking notification opens App
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingOpenApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on Lock Screen
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (mediaInfo != null && mediaInfo.title != "No Media") {
            builder.setContentTitle(mediaInfo.title)
            builder.setContentText(mediaInfo.artist)

            if (mediaInfo.thumbnail != null) {
                try {
                    val decodedString = Base64.decode(mediaInfo.thumbnail, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    builder.setLargeIcon(bitmap)
                } catch (e: Exception) { }
            }

            // Buttons
            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Prev", ACTION_PREV))
            if (mediaInfo.is_playing) {
                builder.addAction(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
            } else {
                builder.addAction(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY))
            }
            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))

            builder.setStyle(MediaStyle().setShowActionsInCompactView(0, 1, 2))
        } else {
            builder.setContentTitle("Jarvis Active")
            builder.setContentText("Running in background...")
        }

        try {
            startForeground(1, builder.build())
        } catch (e: Exception) {
            Log.e("JarvisService", "Foreground Error: ${e.message}")
        }
    }

    private fun generateAction(icon: Int, title: String, actionStr: String): NotificationCompat.Action {
        val intent = Intent(this, MediaActionReceiver::class.java).apply { action = actionStr }
        val pendingIntent = PendingIntent.getBroadcast(this, actionStr.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Jarvis Service", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceListener.destroy()
        serviceJob.cancel()
        serviceState.value = ServiceState.Stopped
        detailedVoiceState.value = VoiceListener.VoiceState.Stopped
    }

    override fun onBind(intent: Intent?): IBinder? = null
}