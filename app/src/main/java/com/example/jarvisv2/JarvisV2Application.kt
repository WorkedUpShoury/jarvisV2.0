package com.example.jarvisv2

import androidx.multidex.MultiDexApplication // <-- FIX: This import was missing

// This class is required to enable MultiDex
class JarvisV2Application : MultiDexApplication() { // <-- FIX: It extends MultiDexApplication
    override fun onCreate() {
        super.onCreate() // <-- FIX: This was also missing
    }
}