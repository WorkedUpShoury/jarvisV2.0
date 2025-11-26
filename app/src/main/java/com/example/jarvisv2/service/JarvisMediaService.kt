package com.example.jarvisv2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
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

class JarvisMediaService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var apiClient: JarvisApiClient

    // MediaSession is required for Android 11+ controls to appear correctly
    private lateinit var mediaSession: MediaSessionCompat

    companion object {
        // Actions
        const val ACTION_PLAY = "com.example.jarvisv2.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.jarvisv2.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.jarvisv2.ACTION_NEXT"
        const val ACTION_PREV = "com.example.jarvisv2.ACTION_PREV"
        const val ACTION_STOP = "com.example.jarvisv2.ACTION_STOP"

        // Shared State
        var sharedMediaState = MutableStateFlow<MediaStateResponse?>(null)
        var serverUrl: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        apiClient = JarvisApiClient(this)

        // Initialize MediaSession
        mediaSession = MediaSessionCompat(this, "JarvisMediaSession")
        mediaSession.isActive = true // Tells the system this app is playing media

        // Start polling immediately upon creation
        startMediaPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        // Ensure notification is visible
        updateNotification(sharedMediaState.value)
        return START_STICKY
    }

    private fun sendMediaCommand(cmd: String) {
        val url = serverUrl ?: return
        serviceScope.launch(Dispatchers.IO) {
            apiClient.sendCommand(url, cmd)
            delay(500)
            fetchMediaInfo(url)
        }
    }

    private fun startMediaPolling() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val url = serverUrl
                if (url != null) {
                    fetchMediaInfo(url)
                }
                delay(2000)
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
        val channelId = "JarvisMediaChannel"
        createNotificationChannel(channelId)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Configure MediaStyle with the Session Token (Critical for controls)
        var mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingOpenApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (mediaInfo != null && mediaInfo.title != "No Media") {
            builder.setContentTitle(mediaInfo.title)
            builder.setContentText(mediaInfo.artist)

            // Update MediaSession active state
            mediaSession.isActive = true

            if (mediaInfo.thumbnail != null) {
                try {
                    val decodedString = Base64.decode(mediaInfo.thumbnail, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    builder.setLargeIcon(bitmap)
                } catch (e: Exception) {
                    Log.e("MediaService", "Failed to decode/set thumbnail: ${e.message}")
                    builder.setLargeIcon(null as Bitmap?)
                }
            } else {
                builder.setLargeIcon(null as Bitmap?)
            }

            // Add Actions (0: Prev, 1: Play/Pause, 2: Next)
            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Prev", ACTION_PREV))

            if (mediaInfo.is_playing) {
                builder.addAction(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
            } else {
                builder.addAction(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY))
            }

            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))

            // 2. Enable Compact View (shows buttons in collapsed notification)
            mediaStyle = mediaStyle.setShowActionsInCompactView(0, 1, 2)

        } else {
            // Idle state
            builder.setContentTitle("Jarvis Media")
            builder.setContentText("Connected to PC...")
            builder.setLargeIcon(null as Bitmap?)
            mediaSession.isActive = false
        }

        // Apply the style after configuration
        builder.setStyle(mediaStyle)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(101, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(101, builder.build())
            }
        } catch (e: Exception) {
            Log.e("MediaService", "Foreground Error: ${e.message}")
        }
    }

    private fun generateAction(icon: Int, title: String, actionStr: String): NotificationCompat.Action {
        val intent = Intent(this, MediaActionReceiver::class.java).apply { action = actionStr }
        val pendingIntent = PendingIntent.getBroadcast(this, actionStr.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Jarvis Media", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaSession.release() // Important to prevent leaks
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}