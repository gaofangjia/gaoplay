package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.LiveStream
import com.example.data.database.PlayHistory
import com.example.data.database.MediaServer
import com.example.data.repository.WebDavItem
import com.example.data.repository.WebDavClient
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.dlna.DlnaDevice
import com.example.data.repository.LocalVideo
import com.example.data.repository.VideoFolder
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.launch

import com.example.ui.theme.AppIcons

sealed class DashboardTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Videos : DashboardTab("视频", AppIcons.Movie)
    object Folders : DashboardTab("文件夹", AppIcons.Folder)
    object LiveStreams : DashboardTab("直播源", AppIcons.RssFeed)
    object MediaServers : DashboardTab("媒体服务器", AppIcons.Server)
    object HistoryAndCast : DashboardTab("投屏和历史", AppIcons.CastConnected)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: MediaViewModel,
    onPlayVideo: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf<DashboardTab>(DashboardTab.Videos) }
    val isScanningMedia by viewModel.isScanningMedia.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Dialog flags
    var showAddStreamDialog by remember { mutableStateOf(false) }
    var currentSelectedFolder by remember { mutableStateOf<VideoFolder?>(null) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var activeServer by remember { mutableStateOf<MediaServer?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF0F1216))
                    .statusBarsPadding()
            ) {
                // Main Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Logo",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "VidX Player",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { viewModel.scanMediaLibrary() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                        ) {
                            if (isScanningMedia) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Rescan",
                                    tint = Color.White
                                )
                            }
                        }

                        if (selectedTab == DashboardTab.LiveStreams) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showAddStreamDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add IP Stream",
                                    tint = Color.Black
                                )
                            }
                        }

                        if (selectedTab == DashboardTab.MediaServers && activeServer == null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showAddServerDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Server",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }

                // Search Bar (Only shown on relevant tabs)
                if (selectedTab == DashboardTab.Videos) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchVideos(it) },
                        placeholder = { Text("搜索媒体文件...", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchVideos("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                         colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("vidx_search_bar")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F1216),
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                val tabs = listOf(
                    DashboardTab.Videos,
                    DashboardTab.Folders,
                    DashboardTab.LiveStreams,
                    DashboardTab.MediaServers,
                    DashboardTab.HistoryAndCast
                )
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF0B0D10)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                DashboardTab.Videos -> VideosTab(viewModel, onPlayVideo)
                DashboardTab.Folders -> FoldersTab(viewModel) { folder ->
                    currentSelectedFolder = folder
                }
                DashboardTab.LiveStreams -> LiveStreamsTab(viewModel, onPlayVideo)
                DashboardTab.MediaServers -> MediaServersTab(viewModel, onPlayVideo, activeServer, { activeServer = it })
                DashboardTab.HistoryAndCast -> HistoryAndCastTab(viewModel, onPlayVideo)
            }

            // Folder Detail Bottom Sheet Sheet or Dialog overlay
            currentSelectedFolder?.let { folder ->
                FolderVideosDialog(
                    folder = folder,
                    onDismiss = { currentSelectedFolder = null },
                    onPlayVideo = onPlayVideo
                )
            }

            // Livestream injection overlay Dialog
            if (showAddStreamDialog) {
                AddStreamDialog(
                    onDismiss = { showAddStreamDialog = false },
                    onAddStream = { title, url ->
                        viewModel.addLiveStream(title, url)
                        showAddStreamDialog = false
                    }
                )
            }

            // Media Server addition overlay Dialog
            if (showAddServerDialog) {
                AddServerDialog(
                    onDismiss = { showAddServerDialog = false },
                    onAddServer = { name, address, port, username, password ->
                        viewModel.addMediaServer(name, address, port, username, password)
                        showAddServerDialog = false
                    }
                )
            }
        }
    }
}

// ------------------- TABS IMPLEMENTATIONS -------------------

@Composable
fun VideosTab(viewModel: MediaViewModel, onPlayVideo: (String, String) -> Unit) {
    val videos by viewModel.filteredVideos.collectAsState()
    val isScanningMedia by viewModel.isScanningMedia.collectAsState()

    if (videos.isEmpty()) {
        EmptyPlaceholder(
            icon = AppIcons.MovieFilter,
            message = if (isScanningMedia) "正在扫描设备的视频..." else "未找到视频，请尝试点击右上角重新扫描。"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoCardItem(video = video, onClick = { onPlayVideo(video.filePath, video.title) })
            }
        }
    }
}

@Composable
fun FoldersTab(viewModel: MediaViewModel, onFolderClick: (VideoFolder) -> Unit) {
    val folders by viewModel.foldersState.collectAsState()
    val isScanningMedia by viewModel.isScanningMedia.collectAsState()

    if (folders.isEmpty()) {
        EmptyPlaceholder(
            icon = AppIcons.FolderOff,
            message = if (isScanningMedia) "正在搜索本地文件夹..." else "暂无可用的视频文件夹索引。"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(folders, key = { it.folderPath }) { folder ->
                FolderGridItem(folder = folder, onClick = { onFolderClick(folder) })
            }
        }
    }
}

@Composable
fun LiveStreamsTab(viewModel: MediaViewModel, onPlayVideo: (String, String) -> Unit) {
    val streams by viewModel.liveStreams.collectAsState()

    if (streams.isEmpty()) {
        EmptyPlaceholder(
            icon = AppIcons.LiveTv,
            message = "无本地 M3U8 播放列表频道。\n点击右上角的 '+' 按钮，添加您最爱的 HLS IPTV 网络直播源。"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(streams) { stream ->
                StreamCardItem(
                    stream = stream,
                    onPlay = { onPlayVideo(stream.url, stream.title) },
                    onDelete = { viewModel.deleteLiveStream(stream) }
                )
            }
        }
    }
}

@Composable
fun HistoryAndCastTab(viewModel: MediaViewModel, onPlayVideo: (String, String) -> Unit) {
    val history by viewModel.playHistory.collectAsState()
    val castDevices by viewModel.castDevices.collectAsState()
    val isScanningCast by viewModel.isScanningCastDevices.collectAsState()
    val selectedCastDevice by viewModel.selectedCastDevice.collectAsState()

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Section A: DLNA Cast Tooling
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Tv,
                            contentDescription = "DLNA Screen Casting",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "DLNA 智能电视投屏",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { viewModel.scanCastDevices() },
                        enabled = !isScanningCast,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isScanningCast) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("扫描本地电视", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (castDevices.isEmpty()) {
                    Text(
                        text = if (isScanningCast) "正在通过 UDP/SSDP 组播寻找本地 DLNA 电视..." else "未找到屏幕渲染设备。请确保电视与手机连接在同一个 Wi-Fi 网络。",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        items(castDevices) { device ->
                            val isSelected = selectedCastDevice?.id == device.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.selectCastDevice(if (isSelected) null else device)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AppIcons.ConnectedTv,
                                        contentDescription = "TV Device",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = device.name,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (isSelected) {
                                    Text("当前接收端", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section B: Watch History Logs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "播放历史记录",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White
            )

            if (history.isNotEmpty()) {
                Text(
                    text = "清除全部",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { viewModel.clearHistory() }
                )
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无播放历史记录。", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { log ->
                    HistoryCardItem(
                        log = log,
                        onPlay = { onPlayVideo(log.filePath, log.title) },
                        onDelete = { viewModel.deleteHistory(log.filePath) },
                        onCast = {
                            selectedCastDevice?.let { device ->
                                viewModel.castMedia(device, log.filePath, log.title)
                            }
                        },
                        canCast = selectedCastDevice != null
                    )
                }
            }
        }
    }
}

// ------------------- SUB COMPOSABLE CARD ITEMS -------------------

@Composable
fun VideoCardItem(video: LocalVideo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media play representation thumbnail box
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.PlayCircle,
                    contentDescription = "Video Asset",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                // Bottom corner Duration Stamp
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationFormatted,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = video.folderName,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${video.sizeFormatted} • 视频",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = "Play",
                tint = Color.DarkGray
            )
        }
    }
}

@Composable
fun FolderGridItem(folder: VideoFolder, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = AppIcons.FolderCopy,
                contentDescription = "Folder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = folder.folderName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${folder.videoCount} 个视频文件",
                color = Color.Gray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = folder.totalSizeFormatted,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StreamCardItem(stream: LiveStream, onPlay: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play channel", tint = Color.Black)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stream.url,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(AppIcons.DeleteOutline, contentDescription = "Remove stream", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun HistoryCardItem(
    log: PlayHistory,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onCast: () -> Unit,
    canCast: Boolean
) {
    // Watched progress calculation percentage
    val watchPercentage = if (log.duration > 0) log.watchedPosition.toFloat() / log.duration.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "播放进度: 已观看 ${(watchPercentage * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { watchPercentage },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Items
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canCast) {
                    IconButton(onClick = onCast) {
                        Icon(AppIcons.CastConnected, contentDescription = "Cast now", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                IconButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play video", tint = Color.White)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove history item", tint = Color.Gray)
                }
            }
        }
    }
}

// ------------------- AUXILIARY OVERLAYS & DIALOGS -------------------

@Composable
fun EmptyPlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty",
            tint = Color.Gray.copy(alpha = 0.35f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FolderVideosDialog(
    folder: VideoFolder,
    onDismiss: () -> Unit,
    onPlayVideo: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "文件夹: ${folder.folderName}") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(folder.videos) { video ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                onPlayVideo(video.filePath, video.title)
                                onDismiss()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = video.durationFormatted,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun AddStreamDialog(
    onDismiss: () -> Unit,
    onAddStream: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 IPTV HLS 直播源 (M3U8)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("直播源名称") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("M3U8 播放链接 (URL)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotEmpty() && url.isNotEmpty()) onAddStream(title, url) },
                enabled = title.isNotEmpty() && url.isNotEmpty()
            ) {
                Text("确认添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun MediaServersTab(
    viewModel: MediaViewModel,
    onPlayVideo: (String, String) -> Unit,
    activeServer: MediaServer?,
    onActiveServerChange: (MediaServer?) -> Unit
) {
    val servers by viewModel.mediaServers.collectAsState()

    if (activeServer == null) {
        if (servers.isEmpty()) {
            EmptyPlaceholder(
                icon = AppIcons.Server,
                message = "没有保存的媒体服务器。\n点击右上角的 '+' 按钮，输入服务配置及账号密码即可连接。"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    ServerCardItem(
                        server = server,
                        onConnect = { onActiveServerChange(server) },
                        onDelete = { viewModel.deleteMediaServer(server) }
                    )
                }
            }
        }
    } else {
        ServerFileExplorer(
            server = activeServer,
            onBackToServers = { onActiveServerChange(null) },
            onPlayVideo = onPlayVideo
        )
    }
}

@Composable
fun ServerCardItem(
    server: MediaServer,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onConnect),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Server,
                        contentDescription = "Server",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = server.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${server.address}:${server.port}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "用户: ${server.username.ifEmpty { "匿名" }}",
                        color = Color.Gray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Row {
                IconButton(onClick = onConnect) {
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = "Connect",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun ServerFileExplorer(
    server: MediaServer,
    onBackToServers: () -> Unit,
    onPlayVideo: (String, String) -> Unit
) {
    var currentPath by remember { mutableStateOf("/") }
    var items by remember { mutableStateOf<List<WebDavItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val webDavClient = remember { WebDavClient() }
    val scope = rememberCoroutineScope()

    fun loadPath(path: String) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val fullUrl = if (server.port > 0) {
                    "${server.address}:${server.port}"
                } else {
                    server.address
                }
                val result = webDavClient.listFiles(
                    serverUrl = fullUrl,
                    username = server.username,
                    password = server.password,
                    dirPath = path
                )
                items = result
                currentPath = path
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "无法连接媒体服务器。\n请确认服务器支持 WebDAV、地址端口无误、并已连接同一局域网。"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(server) {
        loadPath("/")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D10))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToServers) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = currentPath,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { loadPath(currentPath) }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Error",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { loadPath(currentPath) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("重试连接", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            if (items.isEmpty() && currentPath == "/") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("当前目录中没有找到可播放的媒体文件", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentPath != "/") {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parent = currentPath
                                            .trimEnd('/')
                                            .substringBeforeLast("/")
                                            .ifEmpty { "/" }
                                        loadPath(parent)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Up",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("..", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("返回上一级目录", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = if (item.isDirectory) 0.02f else 0.01f))
                                .clickable {
                                    if (item.isDirectory) {
                                        loadPath(item.path)
                                    } else {
                                        onPlayVideo(item.downloadUrl, item.name)
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) AppIcons.Folder else AppIcons.Movie,
                                contentDescription = "ItemType",
                                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontWeight = if (item.isDirectory) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (item.isDirectory) "文件夹" else item.sizeFormatted,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = "Navigate",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onAddServer: (String, String, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var portStr by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加媒体服务器") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("服务器名称 (如: 我的群晖)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("服务器 IP 或域名") },
                    placeholder = { Text("例: 192.168.1.100") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = portStr,
                    onValueChange = { portStr = it },
                    label = { Text("端口 (WebDAV 默认 5005)") },
                    placeholder = { Text("5005") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("登录用户名") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("登录密码") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && address.isNotEmpty()) {
                        val port = portStr.toIntOrNull() ?: 80
                        onAddServer(name, address, port, username, password)
                    }
                },
                enabled = name.isNotEmpty() && address.isNotEmpty()
            ) {
                Text("确认添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
