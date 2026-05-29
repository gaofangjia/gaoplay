package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "play_history")
data class PlayHistory(
    @PrimaryKey val filePath: String,
    val title: String,
    val duration: Long,
    val watchedPosition: Long,
    val lastWatchedTimestamp: Long,
    val folderPath: String
)

@Entity(tableName = "favorite_media")
data class FavoriteMedia(
    @PrimaryKey val filePath: String,
    val title: String,
    val folderPath: String,
    val addedTimestamp: Long
)

@Entity(tableName = "live_streams")
data class LiveStream(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val category: String = "Default",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface MediaDao {
    // History
    @Query("SELECT * FROM play_history ORDER BY lastWatchedTimestamp DESC")
    fun getPlayHistory(): Flow<List<PlayHistory>>

    @Query("SELECT * FROM play_history WHERE filePath = :filePath LIMIT 1")
    suspend fun getHistoryItem(filePath: String): PlayHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(history: PlayHistory)

    @Query("DELETE FROM play_history WHERE filePath = :filePath")
    suspend fun deleteHistory(filePath: String)

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()

    // Favorites
    @Query("SELECT * FROM favorite_media ORDER BY addedTimestamp DESC")
    fun getFavorites(): Flow<List<FavoriteMedia>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_media WHERE filePath = :filePath)")
    fun isFavoriteFlow(filePath: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_media WHERE filePath = :filePath)")
    suspend fun isFavorite(filePath: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteMedia)

    @Query("DELETE FROM favorite_media WHERE filePath = :filePath")
    suspend fun deleteFavorite(filePath: String)

    // Live Streams (M3U8)
    @Query("SELECT * FROM live_streams ORDER BY addedTimestamp DESC")
    fun getLiveStreams(): Flow<List<LiveStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveStream(stream: LiveStream)

    @Delete
    suspend fun deleteLiveStream(stream: LiveStream)

    // Media Servers
    @Query("SELECT * FROM media_servers ORDER BY addedTimestamp DESC")
    fun getMediaServers(): Flow<List<MediaServer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaServer(server: MediaServer)

    @Delete
    suspend fun deleteMediaServer(server: MediaServer)
}

@Entity(tableName = "media_servers")
data class MediaServer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val port: Int,
    val username: String,
    val password: String,
    val protocol: String = "webdav",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Database(entities = [PlayHistory::class, FavoriteMedia::class, LiveStream::class, MediaServer::class], version = 2, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
