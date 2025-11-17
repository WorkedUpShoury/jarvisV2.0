package com.example.jarvisv2.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun insert(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun clearAll() {
        chatDao.clearAllMessages()
    }

    suspend fun delete(message: ChatMessage) {
        chatDao.deleteMessage(message)
    }
}