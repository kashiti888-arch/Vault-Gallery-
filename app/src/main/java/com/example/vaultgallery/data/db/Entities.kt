package com.example.vaultgallery.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object MediaType {
    const val PHOTO = "PHOTO"
    const val VIDEO = "VIDEO"
    const val DOCUMENT = "DOCUMENT"
    const val PDF = "PDF"
}

object MediaSource {
    const val MEDIASTORE = "MEDIASTORE"
    const val PICKER = "PICKER"
    const val SAF = "SAF"
}

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["mediaType"]),
        Index(value = ["bucketId"]),
        Index(value = ["dateModified"]),
        Index(value = ["mediaType", "isAccessible", "dateModified"]),
        Index(value = ["bucketId", "isAccessible", "dateModified"]),
        Index(value = ["source", "mediaType", "lastSyncedAt"])
    ]
)
data class MediaItem(
    @PrimaryKey val uri: String,
    val displayName: String,
    val mediaType: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val dateModified: Long = 0L,
    val durationMs: Long = 0L,
    val bucketId: String = "",
    val bucketName: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val source: String = MediaSource.MEDIASTORE,
    val isAccessible: Boolean = true,
    val lastSyncedAt: Long = 0L
)

@Entity(
    tableName = "recent_items",
    indices = [Index(value = ["lastAccessedAt"])]
)
data class RecentItem(
    @PrimaryKey val uri: String,
    val mediaType: String,
    val displayName: String,
    val mimeType: String = "",
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val lastPage: Int = 0
)

@Entity(
    tableName = "favorite_items",
    indices = [Index(value = ["mediaType"]), Index(value = ["addedAt"])]
)
data class FavoriteItem(
    @PrimaryKey val uri: String,
    val mediaType: String,
    val displayName: String,
    val mimeType: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["pdfUri"])]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pdfUri: String,
    val pageIndex: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "DARK",
    val fontScale: Float = 1.0f,
    val gridColumns: Int = 3,
    val sortOrder: String = "DATE_DESC",
    val defaultSection: String = "HOME",
    val slideshowIntervalSec: Int = 4,
    val slideshowLoop: Boolean = true,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = true,
    val autoLockTimeoutSec: Int = 60,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AlbumSummary(
    val bucketId: String,
    val bucketName: String,
    val coverUri: String,
    val coverMediaType: String,
    val itemCount: Int,
    val latestDateModified: Long
)
