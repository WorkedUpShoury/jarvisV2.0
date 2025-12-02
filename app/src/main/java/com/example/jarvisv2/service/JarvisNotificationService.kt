package com.example.jarvisv2.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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

        // --- FIX: Recover URL if service restarted by system ---
        if (serverUrl == null) {
            val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            serverUrl = prefs.getString("server_url", null)
        }

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
            .setOngoing(true) // Ensure it sticks
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
                                    // FIX: Only notify if the event happened recently (e.g. last 60s)
                                    if (isEventRecent(event.ts)) {
                                        sendNotification(event.text)
                                    }
                                }
                            }
                            lastEventId = response.last_id
                        }
                    }

                    // --- 2. Poll Specific Reminders Endpoint ---
                    // This catches reminders that might not be in the event stream
                    apiClient.getLatestReminderNotification(url).onSuccess { reminder ->
                        if (reminder.id > lastReminderId) {
                            // FIX: Only notify if the reminder timestamp is recent
                            if (isEventRecent(reminder.ts)) {
                                sendNotification("Reminder: ${reminder.text}")
                            }
                            lastReminderId = reminder.id
                        }
                    }
                }
                delay(2000) // Poll every 2 seconds
            }
        }
    }

    /**
     * Helper to determine if an event/reminder happened recently (within last 60 seconds).
     * This prevents flooding notifications when the app restarts and pulls old history.
     */
    private fun isEventRecent(ts: Double): Boolean {
        val now = System.currentTimeMillis()

        // Heuristic: Check if 'ts' is Seconds or Milliseconds.
        val eventTimeMillis = if (ts < 100_000_000_000) {
            (ts * 1000).toLong()
        } else {
            ts.toLong()
        }

        val diff = now - eventTimeMillis
        // Return true only if the event is less than 60 seconds old
        return diff < 60_000
    }

    private fun sendNotification(message: String) {
        // --- UPDATED LOGIC ---
        // Block notification ONLY IF:
        // 1. App is in foreground
        // 2. AND User is currently on the Chat Screen ("chat")
        if (MainActivity.isAppInForeground && MainActivity.currentRoute == "chat") {
            return
        }

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