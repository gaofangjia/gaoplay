package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.MainDashboard
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    // Manage picture-in-picture reactive state
    private val isInPipFlow = MutableStateFlow(false)
    private var isPlayingVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core ViewModel instantiated via custom parameters provider Factory
        val viewModel by viewModels<MediaViewModel> {
            MediaViewModel.Factory(application)
        }

        setContent {
            MyApplicationTheme {
                val isInPipMode by isInPipFlow.collectAsState()
                var hasStoragePermission by remember { mutableStateOf(checkStoragePermission()) }
                var currentPlayingVideo by remember { mutableStateOf<Pair<String, String>?>(null) }

                // Synchronise local variables to activity constraints
                isPlayingVideo = currentPlayingVideo != null

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasStoragePermission = isGranted
                    if (isGranted) {
                        viewModel.scanMediaLibrary()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1216)
                ) {
                    if (currentPlayingVideo != null) {
                        BackHandler(enabled = true) {
                            currentPlayingVideo = null
                        }
                    }

                    when {
                        // Render video full bleed if playing inside Picture-in-Picture
                        isInPipMode && currentPlayingVideo != null -> {
                            VideoPlayerScreen(
                                videoPath = currentPlayingVideo!!.first,
                                videoTitle = currentPlayingVideo!!.second,
                                viewModel = viewModel,
                                onBack = { currentPlayingVideo = null }
                            )
                        }

                        // Normal view: check for storage permission
                        !hasStoragePermission -> {
                            PermissionRequiredScreen(
                                onRequestPermission = {
                                    val targetPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_VIDEO
                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    requestPermissionLauncher.launch(targetPermission)
                                }
                            )
                        }

                        // Playing standard screen video layout
                        currentPlayingVideo != null -> {
                            VideoPlayerScreen(
                                videoPath = currentPlayingVideo!!.first,
                                videoTitle = currentPlayingVideo!!.second,
                                viewModel = viewModel,
                                onBack = { currentPlayingVideo = null }
                            )
                        }

                        // Render scan library dashboard
                        else -> {
                            MainDashboard(
                                viewModel = viewModel,
                                onPlayVideo = { path, title ->
                                    currentPlayingVideo = Pair(path, title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        val targetPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, targetPermission) == PackageManager.PERMISSION_GRANTED
    }

    // Trigger standard system PiP when user presses home button on an active stream
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayingVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val pipBuilder = PictureInPictureParams.Builder()
                // Standard movies widescreen ratio 16:9
                pipBuilder.setAspectRatio(Rational(16, 9))
                enterPictureInPictureMode(pipBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipFlow.value = isInPictureInPictureMode
    }
}

@Composable
fun PermissionRequiredScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1216))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Permission Security Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "需要存储空间访问权限",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "为了查找并放您本地下载的视频文件，VidX Player 需要访问您设备的媒体库权限。",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = "授予媒体访问权限",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
