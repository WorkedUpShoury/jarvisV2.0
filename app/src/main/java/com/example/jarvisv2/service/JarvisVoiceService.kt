package com.example.jarvisv2.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.jarvisv2.R
import com.example.jarvisv2.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JarvisVoiceService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var voiceListener: VoiceListener

    companion object {
        enum class ServiceState { Running, Stopped, Error }
        var serviceState = MutableStateFlow(ServiceState.Stopped)
        var latestVoiceResult = MutableStateFlow("")
        var detailedVoiceState = MutableStateFlow<VoiceListener.VoiceState>(VoiceListener.VoiceState.Stopped)
    }

    override fun onCreate() {
        super.onCreate()
        voiceListener = VoiceListener(this)
        serviceState.value = ServiceState.Running

        // Voice Listener logic
        voiceListener.voiceState.onEach { state ->
            detailedVoiceState.value = state
            if (state is VoiceListener.VoiceState.Result) {
                val text = state.text.lowercase().trim()

                // MODIFIED: Removed the check for "jarvis" keyword.
                // Now passes all captured speech directly as a command.
                if (text.isNotEmpty()) {
                    latestVoiceResult.value = text
                }
            }
        }.launchIn(serviceScope)

        voiceListener.startListening()
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "JarvisVoiceServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Jarvis Mic", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jarvis Listening")
            .setContentText("Mic is Active")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(102, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(102, notification)
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