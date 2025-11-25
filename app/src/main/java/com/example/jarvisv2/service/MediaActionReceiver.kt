package com.example.jarvisv2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class MediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Forward the action to the Service
        val serviceIntent = Intent(context, JarvisVoiceService::class.java).apply {
            this.action = action
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}