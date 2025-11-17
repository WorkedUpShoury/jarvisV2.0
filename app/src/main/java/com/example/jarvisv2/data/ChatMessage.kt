package com.example.jarvisv2.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.jarvisv2.viewmodel.ChatSender

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val sender: ChatSender,
    val timestamp: Long = System.currentTimeMillis()
)

// Helper to convert ChatSender Enum <-> String for Database
class ChatTypeConverters {
    @TypeConverter
    fun fromSender(sender: ChatSender): String {
        return sender.name
    }

    @TypeConverter
    fun toSender(value: String): ChatSender {
        return try {
            ChatSender.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ChatSender.System // Fallback
        }
    }
}