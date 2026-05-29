package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.database.PlayHistory
import com.example.ui.theme.AppIcons
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import android.content.ContentUris
import android.provider.MediaStore

fun getContentUriFromPath(context: Context, filePath: String): Uri {
    if (filePath.startsWith("content://") || filePath.startsWith("http://") || filePath.startsWith("https://")) {
        return Uri.parse(filePath)
    }
    val file = File(filePath)
    if (!file.exists()) return Uri.fromFile(file)

    val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Video.Media._ID)
    val selection = "${MediaStore.Video.Media.DATA} = ?"
    val selectionArgs = arrayOf(filePath)
    try {
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                return ContentUris.withAppendedId(uri, id)
            }
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return Uri.fromFile(file)
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoPath: String,
    videoTitle: String,
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Retrieve system services
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = context as? Activity

    // Player State
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLocked by remember { mutableStateOf(false) }

    // External Subtitle URI
    var selectedSubtitleUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSubtitleName by remember { mutableStateOf<String?>(null) }

    // Gesture indicator values overlays
    var gestureOverlayBrightness by remember { mutableStateOf<Float?>(null) }
    var gestureOverlayVolume by remember { mutableStateOf<Float?>(null) }
    var gestureOverlaySeekTime by remember { mutableStateOf<Long?>(null) }
    var doubleTapFeedbackText by remember { mutableStateOf<String?>(null) }

    // Dropdowns
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Subtitle Picker Contract
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedSubtitleUri = uri
            selectedSubtitleName = uri.lastPathSegment?.substringAfterLast("/") ?: "Sidecar SRT"
            player?.let { p ->
                val currentPos = p.currentPosition
                val wasPlaying = p.isPlaying
                p.stop()
                
                // Build media item with subtitle injection
                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()

                val mediaUri = getContentUriFromPath(context, videoPath)
                val mediaItem = MediaItem.Builder()
                    .setUri(mediaUri)
                    .setSubtitleConfigurations(listOf(subtitleConfig))
                    .build()

                p.setMediaItem(mediaItem)
                p.prepare()
                p.seekTo(currentPos)
                p.playWhenReady = wasPlaying
            }
        }
    }

    // Initialize ExoPlayer
    DisposableEffect(videoPath) {
        val rawBuilder = ExoPlayer.Builder(context)
        if (videoPath.startsWith("http")) {
            val uri = Uri.parse(videoPath)
            val userInfo = uri.userInfo
            if (!userInfo.isNullOrEmpty()) {
                val decodedUserInfo = try {
                    java.net.URLDecoder.decode(userInfo, "UTF-8")
                } catch (e: Exception) {
                    userInfo
                }
                val authString = android.util.Base64.encodeToString(decodedUserInfo.toByteArray(), android.util.Base64.NO_WRAP)
                val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(mapOf("Authorization" to "Basic $authString"))
                rawBuilder.setMediaSourceFactory(
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(httpDataSourceFactory)
                )
            }
        }
        val rawPlayer = rawBuilder.build().apply {
            val mediaUri = getContentUriFromPath(context, videoPath)
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = true
        }

        // Restore watched duration position from DB
        scope.launch {
            val history = viewModel.getHistoryItem(videoPath)
            if (history != null && history.watchedPosition > 0 && history.watchedPosition < history.duration - 5000) {
                rawPlayer.seekTo(history.watchedPosition)
            }
        }

        player = rawPlayer

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    duration = rawPlayer.duration
                }
            }
        }
        rawPlayer.addListener(listener)

        onDispose {
            rawPlayer.let {
                // Save final play position to DB history
                viewModel.savePosition(
                    filePath = videoPath,
                    title = videoTitle,
                    duration = it.duration,
                    position = it.currentPosition
                )
                it.removeListener(listener)
                it.release()
            }
            player = null
        }
    }

    // Track active position via polling coroutine loop when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                player?.let {
                    currentPosition = it.currentPosition
                }
                delay(500)
            }
        }
    }

    // Auto fade out controls HUD
    LaunchedEffect(showControls) {
        if (showControls && !isLocked) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Android Player View Wrapper
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("vidx_player_surface")
        )

        // Comprehensive Screen Gestures Layer (Transparent, overlays back of HUD)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (isLocked) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                        return@pointerInput
                    }

                    detectTapGestures(
                        onDoubleTap = { offset ->
                            player?.let { p ->
                                val screenWidth = size.width
                                val isRightSide = offset.x > (screenWidth / 2)
                                val seekDelta = if (isRightSide) 10000L else -10000L
                                val newPos = (p.currentPosition + seekDelta).coerceIn(0, duration)
                                p.seekTo(newPos)
                                
                                scope.launch {
                                    doubleTapFeedbackText = if (isRightSide) "▶▶ +10s" else "◀◀ -10s"
                                    delay(800)
                                    doubleTapFeedbackText = null
                                }
                            }
                        },
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
                .pointerInput(isLocked) {
                    if (isLocked) return@pointerInput

                    var lastX = 0f
                    var lastY = 0f
                    var dragDirection = 0 // 1: Horizontal seek, 2: Left drag (brightness), 3: Right drag (volume)
                    var initialSeekingProgress = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            lastX = offset.x
                            lastY = offset.y
                            dragDirection = 0 // Undetermined
                            initialSeekingProgress = player?.currentPosition ?: 0L
                        },
                        onDrag = { change, dragAmount ->
                            val screenWidth = size.width
                            val screenHeight = size.height

                            // Establish major movement axis
                            if (dragDirection == 0) {
                                dragDirection = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    1 // Seek
                                } else if (change.position.x < screenWidth / 2) {
                                    2 // Brightness local control
                                } else {
                                    3 // Volume index control
                                }
                            }

                            when (dragDirection) {
                                1 -> {
                                    // Seeking
                                    val swipePercent = (change.position.x - lastX) / screenWidth
                                    val timeDelta = (swipePercent * duration * 0.35f).toLong() // Dampened swipe
                                    val targetSeek = (initialSeekingProgress + timeDelta).coerceIn(0, duration)
                                    gestureOverlaySeekTime = targetSeek
                                }
                                2 -> {
                                    // Brightness modification (0.0 to 1.0)
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        var currentBright = lp.screenBrightness
                                        if (currentBright < 0) currentBright = 0.5f // Default standard
                                        val brightnessDelta = -dragAmount.y / screenHeight
                                        val targetBright = (currentBright + brightnessDelta).coerceIn(0.01f, 1.0f)
                                        lp.screenBrightness = targetBright
                                        act.window.attributes = lp
                                        gestureOverlayBrightness = targetBright
                                    }
                                }
                                3 -> {
                                    // Volume level setting
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val volDelta = -dragAmount.y / screenHeight
                                    val stepsDelta = (volDelta * maxVolume * 1.5f).toInt()
                                    val targetVol = (curVolume + stepsDelta).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    gestureOverlayVolume = targetVol.toFloat() / maxVolume.toFloat()
                                }
                            }
                        },
                        onDragEnd = {
                            // Apply drag seeking if appropriate
                            if (dragDirection == 1 && gestureOverlaySeekTime != null) {
                                player?.seekTo(gestureOverlaySeekTime!!)
                            }
                            // Reset HUD overlay popups
                            gestureOverlayBrightness = null
                            gestureOverlayVolume = null
                            gestureOverlaySeekTime = null
                        }
                    )
                }
        )

        // Translucent Top and Bottom Vignette overlays
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
        }

        // CONTROL LAYOUT HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // TOP PANEL (Controls lock & navigation header)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = videoTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        selectedSubtitleName?.let { subName ->
                            Text(
                                text = "字幕已挂载: $subName",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (!isLocked) {
                        // Dynamic Subtitle Injection Button
                        IconButton(
                            onClick = { subtitlePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = AppIcons.ClosedCaption,
                                contentDescription = "Add Subtitles",
                                tint = if (selectedSubtitleUri != null) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Playback speed toggle selector
                        IconButton(
                            onClick = { showSpeedDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = AppIcons.Speed,
                                contentDescription = "Speed",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // MIDDLE SECTOR CORES
            // Screen Lock Controller (Left edge always accessible!)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            isLocked = !isLocked
                            showControls = true
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else AppIcons.LockOpen,
                            contentDescription = "Lock Screen Layout",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Center play/pause controls (Hidden when screen is locked)
            if (!isLocked) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AnimatedVisibility(
                        visible = showControls,
                        enter = scaleIn(),
                        exit = scaleOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    player?.let {
                                        val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                                        it.seekTo(newPos)
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = AppIcons.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(32.dp))

                            IconButton(
                                onClick = {
                                    player?.let {
                                        if (it.isPlaying) it.pause() else it.play()
                                    }
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) AppIcons.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(32.dp))

                            IconButton(
                                onClick = {
                                    player?.let {
                                        val newPos = (it.currentPosition + 10000).coerceAtMost(duration)
                                        it.seekTo(newPos)
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = AppIcons.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM PANEL Timeline Controls (Hidden on locked HUD)
            if (!isLocked) {
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Slider and watch labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() else 0f,
                                onValueChange = { newVal ->
                                    currentPosition = newVal.toLong()
                                },
                                onValueChangeFinished = {
                                    player?.seekTo(currentPosition)
                                },
                                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                                    .testTag("vidx_player_slider")
                            )

                            Text(
                                text = formatDuration(duration),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // BRIGHTNESS / VOLUME/ SEEK SWIPE OVERLAYS (Transient in Middle)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Seek drag HUD indicator
            gestureOverlaySeekTime?.let { targetTime ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.FastForward,
                        contentDescription = "Seeking",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${formatDuration(targetTime)} / ${formatDuration(duration)}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Brightness level widget
            gestureOverlayBrightness?.let { bright ->
                VolumeBrightnessHud(
                    icon = AppIcons.Brightness5,
                    title = "亮度",
                    value = bright
                )
            }

            // Volume level widget
            gestureOverlayVolume?.let { vol ->
                VolumeBrightnessHud(
                    icon = if (vol == 0f) AppIcons.VolumeMute else AppIcons.VolumeUp,
                    title = "音量",
                    value = vol
                )
            }

            // Double tap feedback floating toast
            doubleTapFeedbackText?.let { text ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // SPEED MODIFY CHOICE BOTTOM DIALOG
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text(text = "播放倍速设置") },
                text = {
                    Column {
                        val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f)
                        speedOptions.forEach { speed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playbackSpeed = speed
                                        player?.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (speed == 1.0f) "1.0x (正常)" else "${speed}x",
                                    color = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                )
                                if (playbackSpeed == speed) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}

@Composable
fun VolumeBrightnessHud(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { value },
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
