package com.example.jarvisv2.data

import kotlinx.coroutines.flow.Flow

class MediaSearchRepository(private val mediaSearchDao: MediaSearchDao) {

    val recentSearches: Flow<List<MediaSearch>> = mediaSearchDao.getRecentSearches()
    val mostSearchedQuery: Flow<MediaSearch?> = mediaSearchDao.getMostSearchedQuery()

    // Exposed directly for ViewModel's upsert logic
    fun getDao(): MediaSearchDao = mediaSearchDao

    // <--- NEW FUNCTION
    suspend fun clearAll() {
        mediaSearchDao.clearAllSearches()
    }
}