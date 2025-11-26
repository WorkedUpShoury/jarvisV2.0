package com.example.jarvisv2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_searches")
data class MediaSearch(
    @PrimaryKey
    val query: String,
    val source: String, // "spotify" or "youtube"
    val count: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)