package com.example.vaultgallery.data.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaSource
import com.example.vaultgallery.data.db.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(private val context: Context) {

    suspend fun scanDevicePhotos(syncTimestamp: Long = System.currentTimeMillis()): List<MediaItem>? = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        runCatching {
            val results = mutableListOf<MediaItem>()
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                if (idCol < 0) return@use
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateMod = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else 0L
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) else null
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) else null
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0

                    results.add(
                        MediaItem(
                            uri = uri.toString(),
                            displayName = displayName?.ifBlank { "Photo_$id" } ?: "Photo_$id",
                            mediaType = MediaType.PHOTO,
                            mimeType = mime?.ifBlank { "image/*" } ?: "image/*",
                            sizeBytes = size,
                            dateModified = dateMod,
                            durationMs = 0L,
                            bucketId = bucketId ?: "camera_roll",
                            bucketName = bucketName?.ifBlank { "Camera Roll" } ?: "Camera Roll",
                            width = width,
                            height = height,
                            source = MediaSource.MEDIASTORE,
                            isAccessible = true,
                            lastSyncedAt = syncTimestamp
                        )
                    )
                }
            }
            results
        }.getOrNull()
    }

    suspend fun scanDeviceVideos(syncTimestamp: Long = System.currentTimeMillis()): List<MediaItem>? = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        runCatching {
            val results = mutableListOf<MediaItem>()
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                if (idCol < 0) return@use
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateMod = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else 0L
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) else null
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) else null
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0

                    results.add(
                        MediaItem(
                            uri = uri.toString(),
                            displayName = displayName?.ifBlank { "Video_$id" } ?: "Video_$id",
                            mediaType = MediaType.VIDEO,
                            mimeType = mime?.ifBlank { "video/*" } ?: "video/*",
                            sizeBytes = size,
                            dateModified = dateMod,
                            durationMs = duration,
                            bucketId = bucketId ?: "videos",
                            bucketName = bucketName?.ifBlank { "Videos" } ?: "Videos",
                            width = width,
                            height = height,
                            source = MediaSource.MEDIASTORE,
                            isAccessible = true,
                            lastSyncedAt = syncTimestamp
                        )
                    )
                }
            }
            results
        }.getOrNull()
    }

    suspend fun inspectPickedUri(
        uri: Uri,
        source: String,
        fallbackBucketId: String = "imported",
        fallbackBucketName: String = "Imported"
    ): MediaItem? = withContext(Dispatchers.IO) {
        tryPersistReadPermission(uri)
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: guessMimeFromUri(uri)
            var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled"
            var sizeBytes = 0L

            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                            displayName = cursor.getString(nameIdx)
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            sizeBytes = cursor.getLong(sizeIdx)
                        }
                    }
                }

            val mediaType = resolveMediaType(mime, displayName)
            val durationMs = if (mediaType == MediaType.VIDEO) {
                extractVideoDurationMs(uri)
            } else {
                0L
            }
            MediaItem(
                uri = uri.toString(),
                displayName = displayName.ifBlank { "Untitled" },
                mediaType = mediaType,
                mimeType = mime,
                sizeBytes = sizeBytes,
                dateModified = System.currentTimeMillis(),
                durationMs = durationMs,
                bucketId = fallbackBucketId,
                bucketName = fallbackBucketName,
                source = source,
                isAccessible = true
            )
        }.getOrNull()
    }

    private fun extractVideoDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun scanSafFolderTree(treeUri: Uri): List<MediaItem> = withContext(Dispatchers.IO) {
        tryPersistReadWritePermission(treeUri)
        val items = mutableListOf<MediaItem>()
        runCatching {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@runCatching
            val folderName = root.name?.ifBlank { "Linked Folder" } ?: "Linked Folder"
            val bucketId = "saf_${treeUri.toString().hashCode()}"
            val children = root.listFiles()
            for (child in children.take(250)) {
                if (!child.isFile || !child.canRead()) continue
                val name = child.name ?: continue
                val mime = child.type ?: guessMimeFromName(name)
                val mediaType = resolveMediaType(mime, name)
                items.add(
                    MediaItem(
                        uri = child.uri.toString(),
                        displayName = name,
                        mediaType = mediaType,
                        mimeType = mime,
                        sizeBytes = child.length(),
                        dateModified = child.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis(),
                        durationMs = 0L,
                        bucketId = bucketId,
                        bucketName = folderName,
                        source = MediaSource.SAF,
                        isAccessible = true
                    )
                )
            }
        }
        items
    }

    suspend fun isUriAccessible(uriString: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    suspend fun renameSafDocument(uriString: String, newName: String): Uri? = withContext(Dispatchers.IO) {
        val sanitized = newName.trim().replace("/", "_").replace("\\", "_")
        if (sanitized.isBlank()) return@withContext null
        runCatching {
            val uri = Uri.parse(uriString)
            DocumentsContract.renameDocument(context.contentResolver, uri, sanitized)
        }.getOrNull()
    }

    suspend fun deleteSafDocument(uriString: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriString)
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                DocumentFile.fromSingleUri(context, uri)?.delete() ?: false
            }
        }.getOrDefault(false)
    }

    fun tryPersistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    fun tryPersistReadWritePermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.onFailure {
            tryPersistReadPermission(uri)
        }
    }

    fun resolveMediaType(mimeType: String, fileName: String): String {
        val lowerMime = mimeType.lowercase()
        val lowerName = fileName.lowercase()
        return when {
            lowerMime == "application/pdf" || lowerName.endsWith(".pdf") -> MediaType.PDF
            lowerMime.startsWith("image/") ||
                lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp") ||
                lowerName.endsWith(".heic") || lowerName.endsWith(".gif") -> MediaType.PHOTO
            lowerMime.startsWith("video/") ||
                lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                lowerName.endsWith(".webm") || lowerName.endsWith(".mov") -> MediaType.VIDEO
            else -> MediaType.DOCUMENT
        }
    }

    private fun guessMimeFromUri(uri: Uri): String {
        return guessMimeFromName(uri.lastPathSegment.orEmpty())
    }

    private fun guessMimeFromName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".md") -> "text/markdown"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".doc") -> "application/msword"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".xls") || lower.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            lower.endsWith(".ppt") || lower.endsWith(".pptx") -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "application/octet-stream"
        }
    }
}
