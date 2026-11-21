package com.example.vaultgallery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vaultgallery.data.db.AlbumSummary
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.ui.components.EmptyVaultState
import com.example.vaultgallery.ui.components.MediaThumbnailCard
import com.example.vaultgallery.ui.components.PermissionBanner

@Composable
fun AlbumsScreen(
    albums: List<AlbumSummary>,
    accessState: MediaAccessState,
    onRequestMediaPermission: () -> Unit,
    onPickMedia: () -> Unit,
    onLinkFolder: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenAlbum: (AlbumSummary) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = remember(maxWidth) {
            when {
                maxWidth >= 900.dp -> 4
                maxWidth >= 600.dp -> 3
                else -> 2
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("albums_grid")
        ) {
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${albums.size} Album${if (albums.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onLinkFolder,
                        modifier = Modifier.testTag("albums_link_folder_button")
                    ) {
                        Icon(
                            Icons.Filled.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Link Folder")
                    }
                }
            }

            if (accessState != MediaAccessState.GRANTED) {
                item(span = { GridItemSpan(columns) }) {
                    PermissionBanner(
                        accessState = accessState,
                        onRequestPermission = onRequestMediaPermission,
                        onPickMedia = onPickMedia,
                        onOpenSettings = onOpenSystemSettings
                    )
                }
            }

            if (albums.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    EmptyVaultState(
                        title = "No Albums Found",
                        body = "Allow media library access to see your device folders, or link any folder via Storage Access Framework.",
                        primaryActionLabel = "Link Folder",
                        onPrimaryAction = onLinkFolder,
                        secondaryActionLabel = "Pick Media",
                        onSecondaryAction = onPickMedia
                    )
                }
            } else {
                items(albums, key = { it.bucketId }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onOpenAlbum(album) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("album_card_${album.bucketId.hashCode()}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            MediaThumbnailCard(
                uri = album.coverUri,
                contentDescription = "${album.bucketName} cover",
                mediaType = album.coverMediaType,
                aspectRatio = 1.05f,
                cornerRadiusDp = 16,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                onClick = onClick
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
            ) {
                Text(
                    text = album.bucketName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${album.itemCount} item${if (album.itemCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    bucketName: String,
    items: List<MediaItem>,
    favoriteUris: Set<String>,
    gridColumns: Int,
    onBack: () -> Unit,
    onOpenPhoto: (MediaItem) -> Unit,
    onOpenVideo: (MediaItem) -> Unit,
    onOpenDocument: (MediaItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = bucketName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${items.size} item${if (items.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("album_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val adaptiveCols = remember(maxWidth, gridColumns) {
                when {
                    maxWidth >= 900.dp -> (gridColumns + 2).coerceIn(4, 7)
                    maxWidth >= 600.dp -> (gridColumns + 1).coerceIn(3, 6)
                    else -> gridColumns.coerceIn(2, 5)
                }
            }
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "This album is empty.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(adaptiveCols),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.uri }) { item ->
                        MediaThumbnailCard(
                            uri = item.uri,
                            contentDescription = item.displayName,
                            mediaType = item.mediaType,
                            durationMs = item.durationMs,
                            isFavorite = item.uri in favoriteUris,
                            aspectRatio = 1f,
                            cornerRadiusDp = 8,
                            onClick = {
                                when (item.mediaType) {
                                    MediaType.PHOTO -> onOpenPhoto(item)
                                    MediaType.VIDEO -> onOpenVideo(item)
                                    else -> onOpenDocument(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
