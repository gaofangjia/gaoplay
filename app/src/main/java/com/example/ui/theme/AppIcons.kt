package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(6f, 19f)
            horizontalLineTo(10f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            close()
            moveTo(14f, 5f)
            verticalLineTo(19f)
            horizontalLineTo(18f)
            verticalLineTo(5f)
            close()
        }.build()
    }

    val Movie: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Movie",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(18f, 4f)
            lineTo(20f, 8f)
            horizontalLineTo(17f)
            lineTo(15f, 4f)
            horizontalLineTo(13f)
            lineTo(15f, 8f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(8f)
            lineTo(10f, 8f)
            horizontalLineTo(7f)
            lineTo(5f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
            verticalLineTo(18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(4f)
            close()
        }.build()
    }

    val Folder: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
            lineTo(2f, 18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 4f, 20f, 4f)
            horizontalLineTo(12f)
            close()
        }.build()
    }

    val RssFeed: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.RssFeed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(4f, 20f)
            horizontalLineTo(6.19f)
            curveTo(6.19f, 12.38f, 12.38f, 6.19f, 20f, 6.19f)
            verticalLineTo(4f)
            curveTo(11.16f, 4f, 4f, 11.16f, 4f, 20f)
            close()
            moveTo(4f, 15.11f)
            curveTo(5.23f, 15.11f, 8.89f, 18.77f, 8.89f, 20f)
            horizontalLineTo(4f)
            close()
        }.build()
    }

    val LiveTv: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.LiveTv",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(21f, 6f)
            horizontalLineTo(3f)
            curveTo(1.9f, 6f, 1f, 6.9f, 1f, 8f)
            verticalLineTo(18f)
            curveTo(1f, 19.1f, 1.9f, 20f, 3f, 20f)
            horizontalLineTo(21f)
            curveTo(22.1f, 20f, 23f, 19.1f, 23f, 18f)
            verticalLineTo(8f)
            curveTo(23f, 6.9f, 22.1f, 6f, 21f, 6f)
            close()
            moveTo(9f, 10f)
            lineTo(16f, 13f)
            lineTo(9f, 16f)
            close()
        }.build()
    }

    val Tv: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Tv",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(21f, 3f)
            horizontalLineTo(3f)
            curveTo(1.9f, 3f, 1f, 3.9f, 1f, 5f)
            verticalLineTo(16f)
            curveTo(1f, 17.1f, 1.9f, 18f, 3f, 18f)
            horizontalLineTo(10f)
            verticalLineTo(20f)
            horizontalLineTo(14f)
            verticalLineTo(18f)
            horizontalLineTo(21f)
            curveTo(22.1f, 18f, 23f, 17.1f, 23f, 16f)
            verticalLineTo(5f)
            curveTo(23f, 3.9f, 22.1f, 3f, 21f, 3f)
            close()
        }.build()
    }

    val ConnectedTv: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.ConnectedTv",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(21f, 3f)
            horizontalLineTo(3f)
            curveTo(1.9f, 3f, 1f, 3.9f, 1f, 5f)
            verticalLineTo(16f)
            curveTo(1f, 17.1f, 1.9f, 18f, 3f, 18f)
            horizontalLineTo(21f)
            curveTo(22.1f, 18f, 23f, 17.1f, 23f, 16f)
            verticalLineTo(5f)
            curveTo(23f, 3.9f, 22.1f, 3f, 21f, 3f)
            close()
            moveTo(2.5f, 13.5f)
            curveTo(4.43f, 13.5f, 6f, 15.07f, 6f, 17f)
            close()
        }.build()
    }

    val CastConnected: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.CastConnected",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(1f, 18f)
            verticalLineTo(21f)
            horizontalLineTo(4f)
            curveTo(4f, 19.34f, 2.66f, 18f, 1f, 18f)
            close()
            moveTo(1f, 14f)
            verticalLineTo(16f)
            curveTo(3.76f, 16f, 6f, 18.24f, 6f, 21f)
            horizontalLineTo(8f)
            curveTo(8f, 17.13f, 4.87f, 14f, 1f, 14f)
            close()
            moveTo(1f, 10f)
            verticalLineTo(12f)
            curveTo(5.97f, 12f, 10f, 16.03f, 10f, 21f)
            horizontalLineTo(12f)
            curveTo(12f, 14.92f, 7.08f, 10f, 1f, 10f)
            close()
            moveTo(21f, 3f)
            horizontalLineTo(3f)
            curveTo(1.9f, 3f, 1f, 3.9f, 1f, 5f)
            verticalLineTo(7f)
            horizontalLineTo(3f)
            verticalLineTo(5f)
            horizontalLineTo(21f)
            verticalLineTo(19f)
            horizontalLineTo(14f)
            verticalLineTo(21f)
            horizontalLineTo(21f)
            curveTo(22.1f, 21f, 23f, 20.1f, 23f, 19f)
            verticalLineTo(5f)
            curveTo(23f, 3.9f, 22.1f, 3f, 21f, 3f)
            close()
        }.build()
    }

    val PlayCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.PlayCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(10f, 16.5f)
            verticalLineTo(7.5f)
            lineTo(16f, 12f)
            close()
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(10f, 6f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12f)
            lineTo(8.59f, 16.59f)
            lineTo(10f, 18f)
            lineTo(16f, 12f)
            close()
        }.build()
    }

    val FolderCopy: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.FolderCopy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(20f, 6f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
            verticalLineTo(16f)
            curveTo(2f, 17.1f, 2.9f, 18f, 4f, 18f)
            horizontalLineTo(20f)
            curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
        }.build()
    }

    val FolderOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.FolderOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(20f, 6f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
            lineTo(2f, 18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
        }.build()
    }

    val DeleteOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.DeleteOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(6f, 19f)
            curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
            horizontalLineTo(16f)
            curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
            verticalLineTo(7f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            close()
            moveTo(8f, 9f)
            horizontalLineTo(16f)
            verticalLineTo(19f)
            horizontalLineTo(8f)
            verticalLineTo(9f)
            close()
            moveTo(15.5f, 4f)
            lineTo(14.5f, 3f)
            horizontalLineTo(9.5f)
            lineTo(8.5f, 4f)
            horizontalLineTo(5f)
            verticalLineTo(6f)
            horizontalLineTo(19f)
            verticalLineTo(4f)
            close()
        }.build()
    }

    val ClosedCaption: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.ClosedCaption",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(19f, 4f)
            horizontalLineTo(5f)
            curveTo(3.89f, 4f, 3f, 4.9f, 3f, 6f)
            verticalLineTo(18f)
            curveTo(3f, 19.1f, 3.89f, 20f, 5f, 20f)
            horizontalLineTo(19f)
            curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f)
            verticalLineTo(6f)
            curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
            close()
        }.build()
    }

    val Speed: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Speed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(20.38f, 8.57f)
            lineTo(18.97f, 9.99f)
            curveTo(19.62f, 11.23f, 20f, 12.59f, 20f, 14f)
            curveTo(20f, 18.42f, 16.42f, 22f, 12f, 22f)
            curveTo(7.58f, 22f, 4f, 18.42f, 4f, 14f)
            curveTo(4f, 9.58f, 7.58f, 6f, 12f, 6f)
            curveTo(13.41f, 6f, 14.77f, 6.38f, 16.01f, 7.03f)
            lineTo(17.43f, 5.61f)
            curveTo(15.86f, 4.6f, 14.01f, 4f, 12f, 4f)
            curveTo(6.48f, 4f, 2f, 8.48f, 2f, 14f)
            curveTo(2f, 19.52f, 6.48f, 24f, 12f, 24f)
            curveTo(17.52f, 24f, 22f, 19.52f, 22f, 14f)
            curveTo(22f, 11.99f, 21.4f, 10.14f, 20.38f, 8.57f)
            close()
        }.build()
    }

    val LockOpen: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.LockOpen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(12f, 17f)
            curveTo(13.1f, 17f, 14f, 16.1f, 14f, 15f)
            curveTo(14f, 13.9f, 13.1f, 13f, 12f, 13f)
            curveTo(10.9f, 13f, 10f, 13.9f, 10f, 15f)
            curveTo(10f, 16.1f, 10.9f, 17f, 12f, 17f)
            close()
            moveTo(18f, 8f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
            horizontalLineTo(9f)
            curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
            curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f)
            curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
            close()
        }.build()
    }

    val Replay10: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Replay10",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(12f, 5f)
            verticalLineTo(1f)
            lineTo(7f, 6f)
            lineTo(12f, 11f)
            verticalLineTo(7f)
            curveTo(15.31f, 7f, 18f, 9.69f, 18f, 13f)
            curveTo(18f, 16.31f, 15.31f, 19f, 12f, 19f)
            curveTo(8.69f, 19f, 6f, 16.31f, 6f, 13f)
            horizontalLineTo(4f)
            curveTo(4f, 17.42f, 7.58f, 21f, 12f, 21f)
            curveTo(16.42f, 21f, 20f, 17.42f, 20f, 13f)
            curveTo(20f, 8.58f, 16.42f, 5f, 12f, 5f)
            close()
        }.build()
    }

    val Forward10: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Forward10",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(12f, 5f)
            verticalLineTo(1f)
            lineTo(17f, 6f)
            lineTo(12f, 11f)
            verticalLineTo(7f)
            curveTo(8.69f, 7f, 6f, 9.69f, 6f, 13f)
            curveTo(6f, 16.31f, 8.69f, 19f, 12f, 19f)
            curveTo(15.31f, 19f, 18f, 16.31f, 18f, 13f)
            horizontalLineTo(20f)
            curveTo(20f, 17.42f, 16.42f, 21f, 12f, 21f)
            curveTo(7.58f, 21f, 4f, 17.42f, 4f, 13f)
            curveTo(4f, 8.58f, 7.58f, 5f, 12f, 5f)
            close()
        }.build()
    }

    val FastForward: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.FastForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(4f, 18f)
            lineTo(12.5f, 12f)
            lineTo(4f, 6f)
            verticalLineTo(18f)
            close()
            moveTo(13f, 6f)
            verticalLineTo(18f)
            lineTo(21.5f, 12f)
            lineTo(13f, 6f)
            close()
        }.build()
    }

    val Brightness5: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Brightness5",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(11.99f, 7.07f)
            curveTo(9.28f, 7.07f, 7.07f, 9.28f, 7.07f, 11.99f)
            curveTo(7.07f, 14.7f, 9.28f, 16.92f, 11.99f, 16.92f)
            curveTo(14.7f, 16.92f, 16.92f, 14.7f, 16.92f, 11.99f)
            curveTo(16.92f, 9.28f, 14.7f, 7.07f, 11.99f, 7.07f)
            close()
        }.build()
    }

    val VolumeMute: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.VolumeMute",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(7f, 9f)
            verticalLineTo(15f)
            horizontalLineTo(11f)
            lineTo(16f, 20f)
            verticalLineTo(4f)
            lineTo(11f, 9f)
            horizontalLineTo(7f)
            close()
        }.build()
    }

    val VolumeUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.VolumeUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(3f, 9f)
            verticalLineTo(15f)
            horizontalLineTo(7f)
            lineTo(12f, 20f)
            verticalLineTo(4f)
            lineTo(7f, 9f)
            horizontalLineTo(3f)
            close()
            moveTo(16.5f, 12f)
            curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f)
            verticalLineTo(16.02f)
            curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
            close()
        }.build()
    }

    val MovieFilter: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.MovieFilter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(18f, 4f)
            lineTo(20f, 8f)
            horizontalLineTo(17f)
            lineTo(15f, 4f)
            horizontalLineTo(13f)
            lineTo(15f, 8f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(8f)
            close()
        }.build()
    }
}
