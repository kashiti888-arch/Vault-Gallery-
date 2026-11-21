package com.example.vaultgallery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.ui.components.EmptyVaultState
import com.example.vaultgallery.ui.components.MediaThumbnailCard
import com.example.vaultgallery.ui.components.PermissionBanner
import com.example.vaultgallery.ui.components.SortOrderMenuButton

@Composable
fun PhotosScreen(
    photos: List<MediaItem>,
    favoriteUris: Set<String>,
    gridColumns: Int,
    sortOrder: String,
    accessState: MediaAccessState,
    onSelectSort: (String) -> Unit,
    onRefresh: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onPickPhotos: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenPhoto: (MediaItem) -> Unit
) {
    var filenameQuery by rememberSaveable { mutableStateOf("") }
    val filteredPhotos = remember(photos, filenameQuery) {
        val q = filenameQuery.trim()
        if (q.isEmpty()) photos
        else photos.filter {
            it.displayName.contains(q, ignoreCase = true) ||
                it.bucketName.contains(q, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Inline filename filter + sort + pick controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filenameQuery,
                onValueChange = { filenameQuery = it },
                placeholder = { Text("Filter photos by filename…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filenameQuery.isNotEmpty()) {
                        IconButton(onClick = { filenameQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear filter")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("photos_filter_input")
            )
            Spacer(modifier = Modifier.width(6.dp))
            SortOrderMenuButton(currentSort = sortOrder, onSelectSort = onSelectSort)
            IconButton(onClick = onRefresh, modifier = Modifier.testTag("photos_refresh_button")) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh photos")
            }
            FilledTonalIconButton(
                onClick = onPickPhotos,
                modifier = Modifier.testTag("photos_pick_button")
            ) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Pick photos",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val adaptiveColumns = remember(maxWidth, gridColumns) {
                when {
                    maxWidth >= 900.dp -> (gridColumns + 2).coerceIn(4, 7)
                    maxWidth >= 600.dp -> (gridColumns + 1).coerceIn(3, 6)
                    else -> gridColumns.coerceIn(2, 5)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(adaptiveColumns),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = 28.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("photos_grid")
            ) {
                if (accessState != MediaAccessState.GRANTED) {
                    item(span = { GridItemSpan(adaptiveColumns) }) {
                        PermissionBanner(
                            accessState = accessState,
                            onRequestPermission = onRequestMediaPermission,
                            onPickMedia = onPickPhotos,
                            onOpenSettings = onOpenSystemSettings
                        )
                    }
                }

                if (filteredPhotos.isEmpty()) {
                    item(span = { GridItemSpan(adaptiveColumns) }) {
                        EmptyVaultState(
                            title = if (filenameQuery.isNotBlank()) "No Matching Photos" else "No Photos Yet",
                            body = if (filenameQuery.isNotBlank()) {
                                "Try another filename or clear the filter."
                            } else {
                                "Grant photo library access to browse device albums or pick photos privately with Photo Picker."
                            },
                            primaryActionLabel = "Pick Photos",
                            onPrimaryAction = onPickPhotos,
                            secondaryActionLabel = if (accessState != MediaAccessState.GRANTED) "Allow Access" else null,
                            onSecondaryAction = if (accessState != MediaAccessState.GRANTED) onRequestMediaPermission else null
                        )
                    }
                } else {
                    items(filteredPhotos, key = { it.uri }) { photo ->
                        MediaThumbnailCard(
                            uri = photo.uri,
                            contentDescription = photo.displayName,
                            mediaType = MediaType.PHOTO,
                            isFavorite = photo.uri in favoriteUris,
                            aspectRatio = 1f,
                            cornerRadiusDp = 8,
                            modifier = Modifier.testTag("photo_item_${photo.uri.hashCode()}"),
                            onClick = { onOpenPhoto(photo) }
                        )
                    }
                }
            }
        }
    }
}
