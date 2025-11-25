package com.example.jarvisv2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class MediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Forward to the NEW Media Service
        val serviceIntent = Intent(context, JarvisMediaService::class.java).apply {
            this.action = action
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}