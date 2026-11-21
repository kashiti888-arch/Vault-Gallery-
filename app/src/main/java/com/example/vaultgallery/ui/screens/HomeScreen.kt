package com.example.vaultgallery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vaultgallery.data.db.FavoriteItem
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.db.RecentItem
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.ui.components.EmptyVaultState
import com.example.vaultgallery.ui.components.MediaThumbnailCard
import com.example.vaultgallery.ui.components.PermissionBanner
import com.example.vaultgallery.ui.components.SectionHeaderRow

@Composable
fun HomeScreen(
    photos: List<MediaItem>,
    videos: List<MediaItem>,
    documents: List<MediaItem>,
    recentDocuments: List<RecentItem>,
    favorites: List<FavoriteItem>,
    favoriteUris: Set<String>,
    accessState: MediaAccessState,
    isSyncing: Boolean = false,
    onOpenSearch: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onPickMedia: () -> Unit,
    onPickDocuments: () -> Unit,
    onLinkFolder: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onSeeAllPhotos: () -> Unit,
    onSeeAllVideos: () -> Unit,
    onSeeAllDocuments: () -> Unit,
    onSeeAllFavorites: () -> Unit,
    onOpenPhoto: (MediaItem) -> Unit,
    onOpenVideo: (MediaItem) -> Unit,
    onOpenDocumentUri: (uri: String, mediaType: String, mimeType: String, name: String) -> Unit
) {
    val recentPhotosList = photos.take(12)
    val recentVideosList = videos.take(10)
    val displayedDocs = if (recentDocuments.isNotEmpty()) {
        recentDocuments.take(8)
    } else {
        documents.take(8).map {
            RecentItem(
                uri = it.uri,
                mediaType = it.mediaType,
                displayName = it.displayName,
                mimeType = it.mimeType,
                lastAccessedAt = it.dateModified
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Tactile Search Bar
        item(key = "search_bar") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenSearch)
                    .testTag("home_search_bar"),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search Vault Gallery",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search photos, videos, PDFs, favorites…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Quick Action Row (Zero-permission Photo Picker & SAF entry points)
        item(key = "quick_actions") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                item {
                    AssistChip(
                        onClick = onPickMedia,
                        label = { Text("Pick Media") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("home_pick_media_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = onPickDocuments,
                        label = { Text("Open PDF / Doc") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("home_open_doc_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = onLinkFolder,
                        label = { Text("Link Folder") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("home_link_folder_chip")
                    )
                }
            }
        }

        // Permission status banner when Denied or Partially Granted
        if (accessState != MediaAccessState.GRANTED) {
            item(key = "permission_banner") {
                PermissionBanner(
                    accessState = accessState,
                    onRequestPermission = onRequestMediaPermission,
                    onPickMedia = onPickMedia,
                    onOpenSettings = onOpenSystemSettings
                )
            }
        }

        // Loading state during initial scan, or clean empty gallery welcome
        if (recentPhotosList.isEmpty() && recentVideosList.isEmpty() &&
            displayedDocs.isEmpty() && favorites.isEmpty()
        ) {
            if (isSyncing) {
                item(key = "loading_home") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Indexing your media library…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item(key = "empty_home") {
                    EmptyVaultState(
                        title = "Your Private Gallery Awaits",
                        body = "Allow device media access to load your albums, or import photos, videos, and PDFs directly with zero-permission pickers.",
                        primaryActionLabel = "Pick Photos & Videos",
                        onPrimaryAction = onPickMedia,
                        secondaryActionLabel = "Open PDF",
                        onSecondaryAction = onPickDocuments
                    )
                }
            }
        }

        // Section 1: Recent Photos
        if (recentPhotosList.isNotEmpty()) {
            item(key = "header_recent_photos") {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeaderRow(
                    title = "Recent Photos",
                    count = photos.size,
                    actionLabel = "See all",
                    onActionClick = onSeeAllPhotos
                )
            }
            item(key = "row_recent_photos") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentPhotosList, key = { it.uri }) { photo ->
                        MediaThumbnailCard(
                            uri = photo.uri,
                            contentDescription = photo.displayName,
                            mediaType = MediaType.PHOTO,
                            isFavorite = photo.uri in favoriteUris,
                            aspectRatio = 0.85f,
                            cornerRadiusDp = 16,
                            modifier = Modifier
                                .width(136.dp)
                                .testTag("home_photo_${photo.uri.hashCode()}"),
                            onClick = { onOpenPhoto(photo) }
                        )
                    }
                }
            }
        }

        // Section 2: Recent Videos
        if (recentVideosList.isNotEmpty()) {
            item(key = "header_recent_videos") {
                Spacer(modifier = Modifier.height(18.dp))
                SectionHeaderRow(
                    title = "Recent Videos",
                    count = videos.size,
                    actionLabel = "See all",
                    onActionClick = onSeeAllVideos
                )
            }
            item(key = "row_recent_videos") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentVideosList, key = { it.uri }) { video ->
                        Column(modifier = Modifier.width(204.dp)) {
                            MediaThumbnailCard(
                                uri = video.uri,
                                contentDescription = video.displayName,
                                mediaType = MediaType.VIDEO,
                                durationMs = video.durationMs,
                                isFavorite = video.uri in favoriteUris,
                                aspectRatio = 16f / 10f,
                                cornerRadiusDp = 16,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_video_${video.uri.hashCode()}"),
                                onClick = { onOpenVideo(video) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = video.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Recent Documents
        if (displayedDocs.isNotEmpty()) {
            item(key = "header_recent_docs") {
                Spacer(modifier = Modifier.height(18.dp))
                SectionHeaderRow(
                    title = "Recent Documents",
                    count = documents.size,
                    actionLabel = "See all",
                    onActionClick = onSeeAllDocuments
                )
            }
            item(key = "row_recent_docs") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedDocs, key = { it.uri }) { doc ->
                        val isPdf = doc.mediaType == MediaType.PDF
                        Surface(
                            modifier = Modifier
                                .width(172.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onOpenDocumentUri(doc.uri, doc.mediaType, doc.mimeType, doc.displayName)
                                }
                                .testTag("home_doc_${doc.uri.hashCode()}"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(
                                            if (isPdf) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPdf) Icons.Filled.PictureAsPdf else Icons.Filled.Description,
                                        contentDescription = doc.displayName,
                                        tint = if (isPdf) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = doc.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isPdf && doc.lastPage > 0) {
                                        "PDF • Page ${doc.lastPage + 1}"
                                    } else if (isPdf) {
                                        "PDF Document"
                                    } else {
                                        "Document"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Favorites
        if (favorites.isNotEmpty()) {
            item(key = "header_favorites") {
                Spacer(modifier = Modifier.height(18.dp))
                SectionHeaderRow(
                    title = "Favorites",
                    count = favorites.size,
                    actionLabel = "See all",
                    onActionClick = onSeeAllFavorites
                )
            }
            item(key = "row_favorites") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favorites.take(12), key = { it.uri }) { fav ->
                        if (fav.mediaType == MediaType.PHOTO || fav.mediaType == MediaType.VIDEO) {
                            MediaThumbnailCard(
                                uri = fav.uri,
                                contentDescription = fav.displayName,
                                mediaType = fav.mediaType,
                                isFavorite = true,
                                aspectRatio = 1f,
                                cornerRadiusDp = 16,
                                modifier = Modifier.width(124.dp),
                                onClick = {
                                    if (fav.mediaType == MediaType.PHOTO) {
                                        onOpenPhoto(
                                            MediaItem(
                                                uri = fav.uri,
                                                displayName = fav.displayName,
                                                mediaType = fav.mediaType,
                                                mimeType = fav.mimeType
                                            )
                                        )
                                    } else {
                                        onOpenVideo(
                                            MediaItem(
                                                uri = fav.uri,
                                                displayName = fav.displayName,
                                                mediaType = fav.mediaType,
                                                mimeType = fav.mimeType
                                            )
                                        )
                                    }
                                }
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(124.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onOpenDocumentUri(fav.uri, fav.mediaType, fav.mimeType, fav.displayName)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            imageVector = if (fav.mediaType == MediaType.PDF) Icons.Filled.PictureAsPdf else Icons.Filled.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = fav.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
