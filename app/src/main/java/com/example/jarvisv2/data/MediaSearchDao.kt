package com.example.jarvisv2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaSearchDao {
    // Inserts a new search or replaces an existing one (which is necessary for updating the count/timestamp)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(search: MediaSearch)

    // Get a flow of the 5 most recently used searches
    @Query("SELECT * FROM media_searches ORDER BY lastUsed DESC LIMIT 5")
    fun getRecentSearches(): Flow<List<MediaSearch>>

    // Get the single most frequently searched query (sorted by count, then by most recent for ties)
    @Query("SELECT * FROM media_searches ORDER BY count DESC, lastUsed DESC LIMIT 1")
    fun getMostSearchedQuery(): Flow<MediaSearch?>
}