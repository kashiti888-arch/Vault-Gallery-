package com.example.vaultgallery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.vaultgallery.data.db.FavoriteItem
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.ui.components.DocumentRowCard
import com.example.vaultgallery.ui.components.EmptyVaultState
import com.example.vaultgallery.ui.components.MediaThumbnailCard

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteItem>,
    gridColumns: Int,
    onOpenPhoto: (FavoriteItem) -> Unit,
    onOpenVideo: (FavoriteItem) -> Unit,
    onOpenDocument: (FavoriteItem) -> Unit,
    onToggleFavorite: (FavoriteItem) -> Unit,
    onShare: (FavoriteItem) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("ALL") }

    val filtered = remember(favorites, filter) {
        when (filter) {
            MediaType.PHOTO -> favorites.filter { it.mediaType == MediaType.PHOTO }
            MediaType.VIDEO -> favorites.filter { it.mediaType == MediaType.VIDEO }
            MediaType.PDF -> favorites.filter { it.mediaType == MediaType.PDF }
            MediaType.DOCUMENT -> favorites.filter { it.mediaType == MediaType.DOCUMENT }
            else -> favorites
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                "ALL" to "All (${favorites.size})",
                MediaType.PHOTO to "Photos (${favorites.count { it.mediaType == MediaType.PHOTO }})",
                MediaType.VIDEO to "Videos (${favorites.count { it.mediaType == MediaType.VIDEO }})",
                MediaType.PDF to "PDFs (${favorites.count { it.mediaType == MediaType.PDF }})",
                MediaType.DOCUMENT to "Docs (${favorites.count { it.mediaType == MediaType.DOCUMENT }})"
            )
            items(chips) { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label) },
                    modifier = Modifier.testTag("favorites_filter_$key")
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyVaultState(
                title = "No Favorites Here Yet",
                body = "Tap the heart icon on any photo, video, PDF, or document to pin it to your Vault Favorites."
            )
        } else if (filter == MediaType.PHOTO || filter == MediaType.VIDEO) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cols = remember(maxWidth, gridColumns) {
                    if (maxWidth >= 600.dp) (gridColumns + 1).coerceIn(3, 6)
                    else gridColumns.coerceIn(2, 4)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("favorites_media_grid")
                ) {
                    items(filtered, key = { it.uri }) { fav ->
                        MediaThumbnailCard(
                            uri = fav.uri,
                            contentDescription = fav.displayName,
                            mediaType = fav.mediaType,
                            isFavorite = true,
                            aspectRatio = 1f,
                            cornerRadiusDp = 12,
                            onClick = {
                                if (fav.mediaType == MediaType.PHOTO) onOpenPhoto(fav)
                                else onOpenVideo(fav)
                            }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_list")
            ) {
                val visualFavs = filtered.filter { it.mediaType == MediaType.PHOTO || it.mediaType == MediaType.VIDEO }
                val docFavs = filtered.filter { it.mediaType == MediaType.PDF || it.mediaType == MediaType.DOCUMENT }

                if (visualFavs.isNotEmpty()) {
                    item(key = "visual_row") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(visualFavs, key = { it.uri }) { fav ->
                                MediaThumbnailCard(
                                    uri = fav.uri,
                                    contentDescription = fav.displayName,
                                    mediaType = fav.mediaType,
                                    isFavorite = true,
                                    aspectRatio = 1f,
                                    cornerRadiusDp = 14,
                                    modifier = Modifier
                                        .width(124.dp)
                                        .padding(vertical = 2.dp),
                                    onClick = {
                                        if (fav.mediaType == MediaType.PHOTO) onOpenPhoto(fav)
                                        else onOpenVideo(fav)
                                    }
                                )
                            }
                        }
                    }
                }

                items(docFavs, key = { it.uri }) { fav ->
                    val asMedia = MediaItem(
                        uri = fav.uri,
                        displayName = fav.displayName,
                        mediaType = fav.mediaType,
                        mimeType = fav.mimeType,
                        dateModified = fav.addedAt
                    )
                    DocumentRowCard(
                        item = asMedia,
                        isFavorite = true,
                        onOpen = { onOpenDocument(fav) },
                        onToggleFavorite = { onToggleFavorite(fav) },
                        onShare = { onShare(fav) }
                    )
                }
            }
        }
    }
}
