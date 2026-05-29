package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.data.database.FavoriteMedia
import com.example.data.database.LiveStream
import com.example.data.database.MediaDao
import com.example.data.database.PlayHistory
import com.example.data.database.MediaServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

data class LocalVideo(
    val id: Long,
    val filePath: String,
    val uriString: String,
    val title: String,
    val folderPath: String,
    val folderName: String,
    val duration: Long,
    val size: Long,
    val dateModified: Long
) {
    val durationFormatted: String
        get() {
            if (duration <= 0) return "00:00"
            val totalSecs = duration / 1000
            val hours = totalSecs / 3600
            val minutes = (totalSecs % 3600) / 60
            val seconds = totalSecs % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val sizeFormatted: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}

data class VideoFolder(
    val folderPath: String,
    val folderName: String,
    val videoCount: Int,
    val totalSize: Long,
    val videos: List<LocalVideo>
) {
    val totalSizeFormatted: String
        get() {
            if (totalSize <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(totalSize.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.2f %s", totalSize / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    // Expose flows from Dao
    val playHistory: Flow<List<PlayHistory>> = mediaDao.getPlayHistory()
    val favorites: Flow<List<FavoriteMedia>> = mediaDao.getFavorites()
    val liveStreams: Flow<List<LiveStream>> = mediaDao.getLiveStreams()

    suspend fun getHistoryItem(filePath: String): PlayHistory? = withContext(Dispatchers.IO) {
        mediaDao.getHistoryItem(filePath)
    }

    suspend fun savePlayPosition(filePath: String, title: String, duration: Long, watchedPosition: Long) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val folderPath = file.parent ?: "/storage/emulated/0"
        val history = PlayHistory(
            filePath = filePath,
            title = title,
            duration = duration,
            watchedPosition = watchedPosition,
            lastWatchedTimestamp = System.currentTimeMillis(),
            folderPath = folderPath
        )
        mediaDao.insertPlayHistory(history)
    }

    suspend fun deleteHistory(filePath: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteHistory(filePath)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        mediaDao.clearHistory()
    }

    fun isFavoriteFlow(filePath: String): Flow<Boolean> = mediaDao.isFavoriteFlow(filePath)

    suspend fun toggleFavorite(filePath: String, title: String) = withContext(Dispatchers.IO) {
        if (mediaDao.isFavorite(filePath)) {
            mediaDao.deleteFavorite(filePath)
        } else {
            val file = File(filePath)
            val folderPath = file.parent ?: "/storage/emulated/0"
            mediaDao.insertFavorite(
                FavoriteMedia(
                    filePath = filePath,
                    title = title,
                    folderPath = folderPath,
                    addedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addLiveStream(title: String, url: String, category: String) = withContext(Dispatchers.IO) {
        mediaDao.insertLiveStream(LiveStream(title = title, url = url, category = category))
    }

    suspend fun deleteLiveStream(stream: LiveStream) = withContext(Dispatchers.IO) {
        mediaDao.deleteLiveStream(stream)
    }

    val mediaServers: Flow<List<MediaServer>> = mediaDao.getMediaServers()

    suspend fun addMediaServer(name: String, address: String, port: Int, username: String, password: String) = withContext(Dispatchers.IO) {
        mediaDao.insertMediaServer(MediaServer(name = name, address = address, port = port, username = username, password = password))
    }

    suspend fun deleteMediaServer(server: MediaServer) = withContext(Dispatchers.IO) {
        mediaDao.deleteMediaServer(server)
    }

    /**
     * High performance media scanner parsing MediaStore index tables for video files
     */
    suspend fun scanLocalVideos(): List<LocalVideo> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<LocalVideo>()
        val contentResolver: ContentResolver = context.contentResolver
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                videoUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val filePath = cursor.getString(dataColumn) ?: ""
                    
                    // Skip files that are non-existent or zero-length
                    if (filePath.isNotEmpty() && !File(filePath).exists()) {
                        continue
                    }

                    val name = cursor.getString(nameColumn) ?: "Untitled Video"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn) * 1000 // Convert to milliseconds

                    val file = File(filePath)
                    val folderPath = file.parent ?: "/storage/emulated/0"
                    val folderName = file.parentFile?.name ?: "Internal Storage"

                    val uri = ContentUris.withAppendedId(videoUri, id).toString()

                    videoList.add(
                        LocalVideo(
                            id = id,
                            filePath = filePath,
                            uriString = uri,
                            title = if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".3gp") || name.endsWith(".mov") || name.endsWith(".ts")) {
                                name.substringBeforeLast(".")
                            } else {
                                name
                            },
                            folderPath = folderPath,
                            folderName = folderName,
                            duration = duration,
                            size = size,
                            dateModified = dateModified
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext videoList
    }

    /**
     * Group individual movies into Folders
     */
    fun groupVideosByFolder(videos: List<LocalVideo>): List<VideoFolder> {
        return videos.groupBy { it.folderPath }
            .map { (folderPath, folderVideos) ->
                val sampleFile = File(folderPath)
                val name = sampleFile.name.ifEmpty { "Internal Storage" }
                VideoFolder(
                    folderPath = folderPath,
                    folderName = name,
                    videoCount = folderVideos.size,
                    totalSize = folderVideos.sumOf { it.size },
                    videos = folderVideos
                )
            }.sortedBy { it.folderName }
    }
}
