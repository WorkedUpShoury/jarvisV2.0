package com.example.jarvisv2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jarvisv2.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JarvisVoiceService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var voiceListener: VoiceListener

    companion object {

        /**
         * ✔ FINAL ENUM — THIS MUST MATCH UI + VIEWMODEL
         */
        enum class ServiceState {
            Running,
            Stopped,
            Error
        }

        /**
         * Public global state
         */
        var serviceState = MutableStateFlow(ServiceState.Stopped)

        /**
         * Latest voice command unwrapped from "Jarvis ..."
         */
        var latestVoiceResult = MutableStateFlow("")
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("JarvisVoiceService", "Service onCreate")

        voiceListener = VoiceListener(this)

        serviceState.value = ServiceState.Running
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("JarvisVoiceService", "Service onStartCommand")
        startForegroundWithNotification()

        voiceListener.voiceState
            .onEach { state ->
                when (state) {

                    is VoiceListener.VoiceState.Result -> {
                        val text = state.text.lowercase().trim()

                        if (text.contains("jarvis")) {
                            val command = text.replace("jarvis", "").trim()
                            if (command.isNotEmpty()) {
                                Log.d("JarvisVoiceService", "Captured command: $command")
                                latestVoiceResult.value = command
                            }
                        }
                    }

                    is VoiceListener.VoiceState.Error -> {
                        Log.e("JarvisVoiceService", "Voice Error: ${state.message}")
                        serviceState.value = ServiceState.Error
                    }

                    else -> Unit
                }
            }
            .launchIn(serviceScope)

        voiceListener.startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("JarvisVoiceService", "Service onDestroy")

        voiceListener.destroy()
        serviceJob.cancel()

        serviceState.value = ServiceState.Stopped
    }

    private fun startForegroundWithNotification() {
        val channelId = "JarvisVoiceServiceChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Jarvis Voice Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("Jarvis is Listening")
                .setContentText("Voice control is active in the background.")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
