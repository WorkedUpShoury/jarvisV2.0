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
import com.example.jarvisv2.network.JarvisApiClient
import com.example.jarvisv2.ui.MainActivity
import kotlinx.coroutines.*

class JarvisNotificationService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var apiClient: JarvisApiClient

    private var lastEventId = 0
    private var lastReminderId = 0 // Track the last reminder we notified about

    companion object {
        var serverUrl: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        apiClient = JarvisApiClient(this)
        startForegroundService()
        startPolling()
    }

    private fun startForegroundService() {
        val channelId = "JarvisMonitorChannel"
        createNotificationChannel(channelId, "Jarvis Monitor", NotificationManager.IMPORTANCE_LOW)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jarvis Active")
            .setContentText("Monitoring for messages & reminders...")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(103, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(103, notification)
        }
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isActive) {
                val url = serverUrl
                if (url != null) {
                    // --- 1. Poll General Events (Messages, etc) ---
                    apiClient.getEvents(url, lastEventId).onSuccess { response ->
                        if (response.last_id > lastEventId) {
                            for (event in response.events) {
                                // "speak" type covers BOTH Agent replies and some Reminders
                                if (event.type == "speak" && event.text.isNotEmpty()) {
                                    sendNotification(event.text)
                                }
                            }
                            lastEventId = response.last_id
                        }
                    }

                    // --- 2. Poll Specific Reminders Endpoint ---
                    // This catches reminders that might not be in the event stream
                    apiClient.getLatestReminderNotification(url).onSuccess { reminder ->
                        if (reminder.id > lastReminderId) {
                            sendNotification("Reminder: ${reminder.text}")
                            lastReminderId = reminder.id
                        }
                    }
                }
                delay(2000) // Poll every 2 seconds
            }
        }
    }

    private fun sendNotification(message: String) {
        val channelId = "JarvisMessageChannel"
        createNotificationChannel(channelId, "Jarvis Messages", NotificationManager.IMPORTANCE_HIGH)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jarvis")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel(id: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}