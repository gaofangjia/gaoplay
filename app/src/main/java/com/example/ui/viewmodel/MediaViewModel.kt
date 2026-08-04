package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.FavoriteMedia
import com.example.data.database.LiveStream
import com.example.data.database.MediaDatabase
import com.example.data.database.PlayHistory
import com.example.data.database.MediaServer
import com.example.data.dlna.DlnaController
import com.example.data.dlna.DlnaDevice
import com.example.data.repository.LocalVideo
import com.example.data.repository.MediaRepository
import com.example.data.repository.VideoFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaViewModel(
    application: Application,
    private val repository: MediaRepository,
    private val dlnaController: DlnaController
) : AndroidViewModel(application) {

    // Scanner state
    private val _isScanningMedia = MutableStateFlow(false)
    val isScanningMedia: StateFlow<Boolean> = _isScanningMedia.asStateFlow()

    private val _allVideosState = MutableStateFlow<List<LocalVideo>>(emptyList())
    val allVideosState: StateFlow<List<LocalVideo>> = _allVideosState.asStateFlow()

    private val _foldersState = MutableStateFlow<List<VideoFolder>>(emptyList())
    val foldersState: StateFlow<List<VideoFolder>> = _foldersState.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Reactively filter videos based on search
    val filteredVideos: StateFlow<List<LocalVideo>> = combine(_allVideosState, _searchQuery) { videos, query ->
        if (query.isBlank()) {
            videos
        } else {
            videos.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DLNA casting
    val castDevices: StateFlow<List<DlnaDevice>> = dlnaController.devices
    val isScanningCastDevices: StateFlow<Boolean> = dlnaController.isScanning
    private val _selectedCastDevice = MutableStateFlow<DlnaDevice?>(null)
    val selectedCastDevice: StateFlow<DlnaDevice?> = _selectedCastDevice.asStateFlow()

    // Play History, Favorites, and Livestreams from Room Database
    val playHistory: StateFlow<List<PlayHistory>> = repository.playHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteMedia>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveStreams: StateFlow<List<LiveStream>> = repository.liveStreams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaServers: StateFlow<List<MediaServer>> = repository.mediaServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial scan of media library
        viewModelScope.launch {
            scanMediaLibrary()
        }
    }

    fun searchVideos(query: String) {
        _searchQuery.value = query
    }

    suspend fun getHistoryItem(filePath: String): PlayHistory? {
        return repository.getHistoryItem(filePath)
    }

    /**
     * Crawls Android device MediaStore to fetch videos and map folders
     */
    fun scanMediaLibrary() {
        viewModelScope.launch {
            _isScanningMedia.value = true
            try {
                val videos = repository.scanLocalVideos()
                _allVideosState.value = videos
                _foldersState.value = repository.groupVideosByFolder(videos)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanningMedia.value = false
            }
        }
    }

    // Database updates triggers
    fun savePosition(filePath: String, title: String, duration: Long, position: Long) {
        viewModelScope.launch {
            repository.savePlayPosition(filePath, title, duration, position)
        }
    }

    fun deleteHistory(filePath: String) {
        viewModelScope.launch {
            repository.deleteHistory(filePath)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun toggleFavorite(filePath: String, title: String) {
        viewModelScope.launch {
            repository.toggleFavorite(filePath, title)
        }
    }

    fun isFavorite(filePath: String): Flow<Boolean> {
        return repository.isFavoriteFlow(filePath)
    }

    // Live m3u8 setups
    fun addLiveStream(title: String, url: String, category: String = "Default Stream") {
        viewModelScope.launch {
            repository.addLiveStream(title, url, category)
        }
    }

    fun addLiveStreams(streams: List<Pair<String, String>>) {
        viewModelScope.launch {
            streams.forEach { (title, url) ->
                repository.addLiveStream(title, url, "M3U Import")
            }
        }
    }

    fun deleteLiveStream(stream: LiveStream) {
        viewModelScope.launch {
            repository.deleteLiveStream(stream)
        }
    }

    fun addMediaServer(name: String, address: String, port: Int, username: String, password: String, protocol: String) {
        viewModelScope.launch {
            repository.addMediaServer(name, address, port, username, password, protocol)
        }
    }

    fun deleteMediaServer(server: MediaServer) {
        viewModelScope.launch {
            repository.deleteMediaServer(server)
        }
    }

    // DLNA discovery routines
    fun scanCastDevices() {
        viewModelScope.launch {
            dlnaController.startScan()
        }
    }

    fun selectCastDevice(device: DlnaDevice?) {
        _selectedCastDevice.value = device
    }

    fun castMedia(device: DlnaDevice, url: String, title: String) {
        viewModelScope.launch {
            dlnaController.castVideo(device, url, title)
        }
    }

    /**
     * Helper manual injector Factory (No DI heavy setup!)
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
                val database = androidx.room.Room.databaseBuilder(
                    application.applicationContext,
                    MediaDatabase::class.java,
                    "vidx_player_db"
                ).fallbackToDestructiveMigration().build()
                val repository = MediaRepository(application.applicationContext, database.mediaDao())
                val dlna = DlnaController(application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MediaViewModel(application, repository, dlna) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
