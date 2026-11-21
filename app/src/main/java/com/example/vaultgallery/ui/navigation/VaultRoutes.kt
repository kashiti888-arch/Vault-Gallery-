package com.example.vaultgallery.ui.navigation

import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import java.nio.charset.StandardCharsets

enum class MainDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showInBottomBar: Boolean
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home, true),
    PHOTOS("photos", "Photos", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary, true),
    ALBUMS("albums", "Albums", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy, true),
    VIDEOS("videos", "Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, true),
    DOCUMENTS("documents", "Documents", Icons.Filled.Description, Icons.Outlined.Description, true),
    FAVORITES("favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, false),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, false);

    companion object {
        fun fromSettingKey(key: String): MainDestination {
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: HOME
        }
    }
}

object VaultRoutes {
    const val SEARCH = "search"
    const val ALBUM_DETAIL = "album/{bucketId}/{bucketName}"
    const val PHOTO_VIEWER = "photo_viewer/{encodedUri}/{bucketId}"
    const val VIDEO_PLAYER = "video_player/{encodedUri}"
    const val PDF_READER = "pdf_reader/{encodedUri}"

    /**
     * URL-safe Base64 encoding prevents Navigation Compose from double-decoding
     * Storage Access Framework document URIs containing %3A / %2F escapes.
     */
    fun encodeArg(value: String): String {
        return Base64.encodeToString(
            value.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun decodeArg(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        return runCatching {
            String(
                Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                StandardCharsets.UTF_8
            )
        }.getOrDefault(encoded)
    }

    fun albumDetail(bucketId: String, bucketName: String): String {
        return "album/${encodeArg(bucketId)}/${encodeArg(bucketName)}"
    }

    fun photoViewer(uri: String, bucketId: String = "ALL"): String {
        return "photo_viewer/${encodeArg(uri)}/${encodeArg(bucketId)}"
    }

    fun videoPlayer(uri: String): String {
        return "video_player/${encodeArg(uri)}"
    }

    fun pdfReader(uri: String): String {
        return "pdf_reader/${encodeArg(uri)}"
    }
}
