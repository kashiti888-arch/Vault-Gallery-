package com.example.vaultgallery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType AND isAccessible = 1 ORDER BY dateModified DESC")
    fun observeByType(mediaType: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE mediaType IN (:types) AND isAccessible = 1 ORDER BY dateModified DESC")
    fun observeByTypes(types: List<String>): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE bucketId = :bucketId AND isAccessible = 1 ORDER BY dateModified DESC")
    fun observeByBucket(bucketId: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isAccessible = 1 ORDER BY dateModified DESC")
    fun observeAllAccessible(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): MediaItem?

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType AND source = :source")
    suspend fun getByTypeAndSource(mediaType: String, source: String): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE isAccessible = 1 AND (displayName LIKE '%' || :escapedQuery || '%' ESCAPE '\\' COLLATE NOCASE OR bucketName LIKE '%' || :escapedQuery || '%' ESCAPE '\\' COLLATE NOCASE) ORDER BY dateModified DESC LIMIT 120")
    fun search(escapedQuery: String): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaItem)

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM media_items WHERE source = :source AND mediaType = :mediaType AND lastSyncedAt < :syncTimestamp")
    suspend fun pruneStaleMediaStoreItems(source: String, mediaType: String, syncTimestamp: Long)

    @Query("DELETE FROM media_items WHERE source = :source AND mediaType = :mediaType")
    suspend fun deleteBySourceAndType(source: String, mediaType: String)

    /**
     * Atomically upserts large MediaStore libraries in 250-item chunks and prunes removed entries
     * in a single SQLite transaction, avoiding the 999 bind-variable cap and preventing mid-sync UI flicker.
     */
    @Transaction
    suspend fun replaceMediaStoreBatch(
        source: String,
        mediaType: String,
        items: List<MediaItem>,
        syncTimestamp: Long
    ) {
        if (items.isEmpty()) {
            deleteBySourceAndType(source, mediaType)
        } else {
            items.chunked(250).forEach { chunk ->
                upsertAll(chunk)
            }
            pruneStaleMediaStoreItems(source, mediaType, syncTimestamp)
        }
    }

    @Query("UPDATE media_items SET displayName = :newName, uri = :newUri WHERE uri = :oldUri")
    suspend fun updateUriAndName(oldUri: String, newUri: String, newName: String)

    @Query("UPDATE media_items SET isAccessible = :accessible WHERE uri = :uri")
    suspend fun setAccessible(uri: String, accessible: Boolean)
}

@Dao
interface RecentItemDao {
    @Query("SELECT * FROM recent_items ORDER BY lastAccessedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 40): Flow<List<RecentItem>>

    @Query("SELECT * FROM recent_items WHERE mediaType IN (:types) ORDER BY lastAccessedAt DESC LIMIT :limit")
    fun observeRecentByTypes(types: List<String>, limit: Int = 20): Flow<List<RecentItem>>

    @Query("SELECT * FROM recent_items WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): RecentItem?

    @Query("SELECT * FROM recent_items WHERE displayName LIKE '%' || :escapedQuery || '%' ESCAPE '\\' COLLATE NOCASE ORDER BY lastAccessedAt DESC LIMIT 50")
    fun search(escapedQuery: String): Flow<List<RecentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RecentItem)

    @Query("DELETE FROM recent_items WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("UPDATE recent_items SET uri = :newUri, displayName = :newName WHERE uri = :oldUri")
    suspend fun updateUriAndName(oldUri: String, newUri: String, newName: String)
}

@Dao
interface FavoriteItemDao {
    @Query("SELECT * FROM favorite_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteItem>>

    @Query("SELECT uri FROM favorite_items")
    fun observeFavoriteUris(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_items WHERE uri = :uri)")
    fun observeIsFavorite(uri: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_items WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Query("SELECT * FROM favorite_items WHERE displayName LIKE '%' || :escapedQuery || '%' ESCAPE '\\' COLLATE NOCASE ORDER BY addedAt DESC LIMIT 50")
    fun search(escapedQuery: String): Flow<List<FavoriteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteItem)

    @Query("DELETE FROM favorite_items WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("UPDATE favorite_items SET uri = :newUri, displayName = :newName WHERE uri = :oldUri")
    suspend fun updateUriAndName(oldUri: String, newUri: String, newName: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE pdfUri = :pdfUri ORDER BY pageIndex ASC")
    fun observeForPdf(pdfUri: String): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE pdfUri = :pdfUri AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getAtPage(pdfUri: String, pageIndex: Int): Bookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE pdfUri = :pdfUri AND pageIndex = :pageIndex")
    suspend fun deleteAtPage(pdfUri: String, pageIndex: Int)

    @Query("DELETE FROM bookmarks WHERE pdfUri = :pdfUri")
    suspend fun deleteForPdf(pdfUri: String)

    @Query("UPDATE bookmarks SET pdfUri = :newUri WHERE pdfUri = :oldUri")
    suspend fun updatePdfUri(oldUri: String, newUri: String)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettings)
}
