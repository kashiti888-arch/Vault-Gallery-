package com.example.vaultgallery.data.repository

import android.content.Context
import android.net.Uri
import com.example.vaultgallery.data.db.AlbumSummary
import com.example.vaultgallery.data.db.Bookmark
import com.example.vaultgallery.data.db.BookmarkDao
import com.example.vaultgallery.data.db.FavoriteItem
import com.example.vaultgallery.data.db.FavoriteItemDao
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaItemDao
import com.example.vaultgallery.data.db.MediaSource
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.db.RecentItem
import com.example.vaultgallery.data.db.RecentItemDao
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.data.media.MediaPermissionManager
import com.example.vaultgallery.data.media.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class GroupedSearchResults(
    val photos: List<MediaItem> = emptyList(),
    val videos: List<MediaItem> = emptyList(),
    val pdfs: List<MediaItem> = emptyList(),
    val documents: List<MediaItem> = emptyList(),
    val favorites: List<FavoriteItem> = emptyList(),
    val recents: List<RecentItem> = emptyList()
) {
    val totalCount: Int
        get() = photos.size + videos.size + pdfs.size + documents.size + favorites.size + recents.size
}

class GalleryRepository(
    private val context: Context,
    private val scanner: MediaStoreScanner,
    private val mediaItemDao: MediaItemDao,
    private val recentItemDao: RecentItemDao,
    private val favoriteItemDao: FavoriteItemDao,
    private val bookmarkDao: BookmarkDao
) {
    private val syncMutex = Mutex()

    val photos: Flow<List<MediaItem>> = mediaItemDao.observeByType(MediaType.PHOTO)
    val videos: Flow<List<MediaItem>> = mediaItemDao.observeByType(MediaType.VIDEO)
    val documentsAndPdfs: Flow<List<MediaItem>> = mediaItemDao.observeByTypes(
        listOf(MediaType.PDF, MediaType.DOCUMENT)
    )
    val allMedia: Flow<List<MediaItem>> = mediaItemDao.observeAllAccessible()
    val favorites: Flow<List<FavoriteItem>> = favoriteItemDao.observeAll()
    val favoriteUris: Flow<Set<String>> = favoriteItemDao.observeFavoriteUris().map { it.toSet() }
    val recentItems: Flow<List<RecentItem>> = recentItemDao.observeRecent(40)
    val recentDocuments: Flow<List<RecentItem>> = recentItemDao.observeRecentByTypes(
        listOf(MediaType.PDF, MediaType.DOCUMENT),
        limit = 16
    )

    val albums: Flow<List<AlbumSummary>> = mediaItemDao
        .observeByTypes(listOf(MediaType.PHOTO, MediaType.VIDEO))
        .map { items ->
            items.groupBy { it.bucketId.ifBlank { "default_album" } }
                .map { (bucketId, bucketItems) ->
                    val sorted = bucketItems.sortedByDescending { it.dateModified }
                    val cover = sorted.first()
                    AlbumSummary(
                        bucketId = bucketId,
                        bucketName = cover.bucketName.ifBlank { "Gallery" },
                        coverUri = cover.uri,
                        coverMediaType = cover.mediaType,
                        itemCount = bucketItems.size,
                        latestDateModified = cover.dateModified
                    )
                }
                .sortedByDescending { it.latestDateModified }
        }
        .flowOn(Dispatchers.Default)

    fun observeAlbumItems(bucketId: String): Flow<List<MediaItem>> = mediaItemDao.observeByBucket(bucketId)

    fun observeIsFavorite(uri: String): Flow<Boolean> = favoriteItemDao.observeIsFavorite(uri)

    fun observeBookmarks(pdfUri: String): Flow<List<Bookmark>> = bookmarkDao.observeForPdf(pdfUri)

    fun searchGlobal(rawQuery: String): Flow<GroupedSearchResults> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return flowOf(GroupedSearchResults())
        val escaped = escapeLikeQuery(query)
        return combine(
            mediaItemDao.search(escaped),
            favoriteItemDao.search(escaped),
            recentItemDao.search(escaped)
        ) { matchedMedia, matchedFavs, matchedRecents ->
            GroupedSearchResults(
                photos = matchedMedia.filter { it.mediaType == MediaType.PHOTO },
                videos = matchedMedia.filter { it.mediaType == MediaType.VIDEO },
                pdfs = matchedMedia.filter { it.mediaType == MediaType.PDF },
                documents = matchedMedia.filter { it.mediaType == MediaType.DOCUMENT },
                favorites = matchedFavs,
                recents = matchedRecents
            )
        }
    }

    private fun escapeLikeQuery(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    suspend fun syncDeviceMedia(): Int = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val accessState = MediaPermissionManager.checkMediaAccessState(context)
            if (accessState == MediaAccessState.DENIED) {
                // Permission denied or revoked: remove stale MediaStore entries while preserving SAF/Picker items
                mediaItemDao.deleteBySourceAndType(MediaSource.MEDIASTORE, MediaType.PHOTO)
                mediaItemDao.deleteBySourceAndType(MediaSource.MEDIASTORE, MediaType.VIDEO)
                return@withLock 0
            }

            val syncTimestamp = System.currentTimeMillis()
            var syncedCount = 0

            if (MediaPermissionManager.canReadImages(context)) {
                val photos = scanner.scanDevicePhotos(syncTimestamp)
                if (photos != null) {
                    mediaItemDao.replaceMediaStoreBatch(
                        source = MediaSource.MEDIASTORE,
                        mediaType = MediaType.PHOTO,
                        items = photos,
                        syncTimestamp = syncTimestamp
                    )
                    syncedCount += photos.size
                }
            } else {
                mediaItemDao.deleteBySourceAndType(MediaSource.MEDIASTORE, MediaType.PHOTO)
            }

            if (MediaPermissionManager.canReadVideos(context)) {
                val videos = scanner.scanDeviceVideos(syncTimestamp)
                if (videos != null) {
                    mediaItemDao.replaceMediaStoreBatch(
                        source = MediaSource.MEDIASTORE,
                        mediaType = MediaType.VIDEO,
                        items = videos,
                        syncTimestamp = syncTimestamp
                    )
                    syncedCount += videos.size
                }
            } else {
                mediaItemDao.deleteBySourceAndType(MediaSource.MEDIASTORE, MediaType.VIDEO)
            }

            syncedCount
        }
    }

    suspend fun importPickedMedia(uris: List<Uri>, bucketName: String = "Picked Media"): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val imported = uris.mapNotNull { uri ->
                scanner.inspectPickedUri(
                    uri = uri,
                    source = MediaSource.PICKER,
                    fallbackBucketId = "picker_${bucketName.lowercase().replace(" ", "_")}",
                    fallbackBucketName = bucketName
                )
            }
            if (imported.isNotEmpty()) {
                imported.chunked(250).forEach { mediaItemDao.upsertAll(it) }
            }
            imported
        }

    suspend fun importSafDocuments(uris: List<Uri>): List<MediaItem> = withContext(Dispatchers.IO) {
        val imported = uris.mapNotNull { uri ->
            scanner.inspectPickedUri(
                uri = uri,
                source = MediaSource.SAF,
                fallbackBucketId = "saf_documents",
                fallbackBucketName = "Documents"
            )
        }
        if (imported.isNotEmpty()) {
            imported.chunked(250).forEach { mediaItemDao.upsertAll(it) }
            imported.forEach { item ->
                recordRecentAccess(item.uri, item.mediaType, item.displayName, item.mimeType)
            }
        }
        imported
    }

    suspend fun importSafFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val items = scanner.scanSafFolderTree(treeUri)
        if (items.isNotEmpty()) {
            items.chunked(100).forEach { mediaItemDao.upsertAll(it) }
        }
        items.size
    }

    suspend fun getMediaItem(uri: String): MediaItem? = withContext(Dispatchers.IO) {
        mediaItemDao.getByUri(uri)
    }

    suspend fun verifyAccessibility(uri: String): Boolean = withContext(Dispatchers.IO) {
        val ok = scanner.isUriAccessible(uri)
        if (!ok) {
            mediaItemDao.setAccessible(uri, false)
        }
        ok
    }

    suspend fun recordRecentAccess(
        uri: String,
        mediaType: String,
        displayName: String,
        mimeType: String = "",
        lastPage: Int? = null
    ) = withContext(Dispatchers.IO) {
        val existing = recentItemDao.getByUri(uri)
        recentItemDao.upsert(
            RecentItem(
                uri = uri,
                mediaType = mediaType,
                displayName = displayName,
                mimeType = mimeType.ifBlank { existing?.mimeType.orEmpty() },
                lastAccessedAt = System.currentTimeMillis(),
                lastPage = lastPage ?: existing?.lastPage ?: 0
            )
        )
    }

    suspend fun getLastPdfPage(uri: String): Int = withContext(Dispatchers.IO) {
        recentItemDao.getByUri(uri)?.lastPage ?: 0
    }

    suspend fun toggleFavorite(
        uri: String,
        mediaType: String,
        displayName: String,
        mimeType: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val exists = favoriteItemDao.isFavorite(uri)
        if (exists) {
            favoriteItemDao.deleteByUri(uri)
            false
        } else {
            favoriteItemDao.upsert(
                FavoriteItem(
                    uri = uri,
                    mediaType = mediaType,
                    displayName = displayName,
                    mimeType = mimeType,
                    addedAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun togglePdfBookmark(pdfUri: String, pageIndex: Int, customLabel: String = ""): Boolean =
        withContext(Dispatchers.IO) {
            val existing = bookmarkDao.getAtPage(pdfUri, pageIndex)
            if (existing != null) {
                bookmarkDao.deleteById(existing.id)
                false
            } else {
                val label = customLabel.ifBlank { "Page ${pageIndex + 1}" }
                bookmarkDao.insert(
                    Bookmark(
                        pdfUri = pdfUri,
                        pageIndex = pageIndex,
                        label = label,
                        createdAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }

    suspend fun deleteBookmark(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteById(id)
    }

    suspend fun renameDocument(item: MediaItem, newName: String): Boolean = withContext(Dispatchers.IO) {
        val renamedUri = scanner.renameSafDocument(item.uri, newName) ?: return@withContext false
        val newUriStr = renamedUri.toString()
        val finalName = newName.trim()
        mediaItemDao.updateUriAndName(item.uri, newUriStr, finalName)
        recentItemDao.updateUriAndName(item.uri, newUriStr, finalName)
        favoriteItemDao.updateUriAndName(item.uri, newUriStr, finalName)
        bookmarkDao.updatePdfUri(item.uri, newUriStr)
        true
    }

    suspend fun deleteSafItem(uri: String): Boolean = withContext(Dispatchers.IO) {
        val deleted = scanner.deleteSafDocument(uri)
        if (deleted || !scanner.isUriAccessible(uri)) {
            removeLocalRecords(uri)
        }
        deleted
    }

    suspend fun removeLocalRecords(uri: String) = withContext(Dispatchers.IO) {
        mediaItemDao.deleteByUri(uri)
        recentItemDao.deleteByUri(uri)
        favoriteItemDao.deleteByUri(uri)
        bookmarkDao.deleteForPdf(uri)
    }
}
