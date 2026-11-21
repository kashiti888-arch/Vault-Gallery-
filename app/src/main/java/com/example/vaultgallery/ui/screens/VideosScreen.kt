package com.example.vaultgallery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.ui.components.EmptyVaultState
import com.example.vaultgallery.ui.components.MediaThumbnailCard
import com.example.vaultgallery.ui.components.PermissionBanner
import com.example.vaultgallery.ui.components.SortOrderMenuButton
import com.example.vaultgallery.util.FormatUtils

@Composable
fun VideosScreen(
    videos: List<MediaItem>,
    favoriteUris: Set<String>,
    sortOrder: String,
    accessState: MediaAccessState,
    onSelectSort: (String) -> Unit,
    onRequestMediaPermission: () -> Unit,
    onPickVideos: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenVideo: (MediaItem) -> Unit,
    onShareVideo: (MediaItem) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = remember(maxWidth) {
            when {
                maxWidth >= 900.dp -> 3
                maxWidth >= 600.dp -> 2
                else -> 1
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("videos_grid")
        ) {
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${videos.size} Video${if (videos.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SortOrderMenuButton(currentSort = sortOrder, onSelectSort = onSelectSort)
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = onPickVideos,
                            modifier = Modifier.testTag("videos_pick_button")
                        ) {
                            Icon(
                                Icons.Filled.VideoCall,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Videos")
                        }
                    }
                }
            }

            if (accessState != MediaAccessState.GRANTED) {
                item(span = { GridItemSpan(columns) }) {
                    PermissionBanner(
                        accessState = accessState,
                        onRequestPermission = onRequestMediaPermission,
                        onPickMedia = onPickVideos,
                        onOpenSettings = onOpenSystemSettings
                    )
                }
            }

            if (videos.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    EmptyVaultState(
                        title = "No Videos Yet",
                        body = "Allow device media access or pick videos to play them with full-screen controls, seek, volume, and rotation.",
                        primaryActionLabel = "Pick Videos",
                        onPrimaryAction = onPickVideos,
                        secondaryActionLabel = if (accessState != MediaAccessState.GRANTED) "Allow Access" else null,
                        onSecondaryAction = if (accessState != MediaAccessState.GRANTED) onRequestMediaPermission else null
                    )
                }
            } else {
                items(videos, key = { it.uri }) { video ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("video_card_${video.uri.hashCode()}")
                    ) {
                        Column {
                            MediaThumbnailCard(
                                uri = video.uri,
                                contentDescription = video.displayName,
                                mediaType = MediaType.VIDEO,
                                durationMs = video.durationMs,
                                isFavorite = video.uri in favoriteUris,
                                aspectRatio = 16f / 9.5f,
                                cornerRadiusDp = 16,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                onClick = { onOpenVideo(video) }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${FormatUtils.formatDuration(video.durationMs)} • ${FormatUtils.formatFileSize(video.sizeBytes)} • ${FormatUtils.formatDate(video.dateModified)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onShareVideo(video) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share video",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
