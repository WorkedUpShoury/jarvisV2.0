package com.example.jarvisv2.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun insert(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    // Kept for initial synchronization/clearing the local cache on connect/server-side delete
    suspend fun clearAll() {
        chatDao.clearAllMessages()
    }

    // Removed: suspend fun delete(message: ChatMessage)
}