package com.example.vaultgallery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.repository.GroupedSearchResults
import com.example.vaultgallery.ui.components.DocumentRowCard
import com.example.vaultgallery.ui.components.EmptyVaultState
import com.example.vaultgallery.ui.components.MediaThumbnailCard
import com.example.vaultgallery.ui.components.SectionHeaderRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: GroupedSearchResults,
    favoriteUris: Set<String>,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenPhoto: (MediaItem) -> Unit,
    onOpenVideo: (MediaItem) -> Unit,
    onOpenDocumentUri: (uri: String, mediaType: String, mimeType: String, name: String) -> Unit,
    onToggleFavorite: (uri: String, mediaType: String, name: String, mimeType: String) -> Unit,
    onShareUri: (uri: String, mimeType: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Vault Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search photos, videos, PDFs, docs, favorites…") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("global_search_input")
            )

            if (query.isBlank()) {
                EmptyVaultState(
                    title = "Global Instant Search",
                    body = "Type a filename, album name, PDF title, or document name to search across your entire gallery."
                )
            } else if (results.totalCount == 0) {
                EmptyVaultState(
                    title = "No Results for \"$query\"",
                    body = "Try another keyword or check your linked albums and documents."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_list")
                ) {
                    if (results.photos.isNotEmpty()) {
                        item(key = "cat_photos") {
                            SectionHeaderRow(title = "Photos", count = results.photos.size, actionLabel = null)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(results.photos, key = { it.uri }) { photo ->
                                    MediaThumbnailCard(
                                        uri = photo.uri,
                                        contentDescription = photo.displayName,
                                        mediaType = MediaType.PHOTO,
                                        isFavorite = photo.uri in favoriteUris,
                                        modifier = Modifier.width(120.dp),
                                        onClick = { onOpenPhoto(photo) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (results.videos.isNotEmpty()) {
                        item(key = "cat_videos") {
                            SectionHeaderRow(title = "Videos", count = results.videos.size, actionLabel = null)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(results.videos, key = { it.uri }) { video ->
                                    MediaThumbnailCard(
                                        uri = video.uri,
                                        contentDescription = video.displayName,
                                        mediaType = MediaType.VIDEO,
                                        durationMs = video.durationMs,
                                        isFavorite = video.uri in favoriteUris,
                                        aspectRatio = 16f / 10f,
                                        modifier = Modifier.width(180.dp),
                                        onClick = { onOpenVideo(video) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (results.pdfs.isNotEmpty()) {
                        item(key = "cat_pdfs_header") {
                            SectionHeaderRow(title = "PDFs", count = results.pdfs.size, actionLabel = null)
                        }
                        items(results.pdfs, key = { "pdf_${it.uri}" }) { pdf ->
                            DocumentRowCard(
                                item = pdf,
                                isFavorite = pdf.uri in favoriteUris,
                                onOpen = { onOpenDocumentUri(pdf.uri, MediaType.PDF, pdf.mimeType, pdf.displayName) },
                                onToggleFavorite = { onToggleFavorite(pdf.uri, MediaType.PDF, pdf.displayName, pdf.mimeType) },
                                onShare = { onShareUri(pdf.uri, pdf.mimeType) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (results.documents.isNotEmpty()) {
                        item(key = "cat_docs_header") {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeaderRow(title = "Documents", count = results.documents.size, actionLabel = null)
                        }
                        items(results.documents, key = { "doc_${it.uri}" }) { doc ->
                            DocumentRowCard(
                                item = doc,
                                isFavorite = doc.uri in favoriteUris,
                                onOpen = { onOpenDocumentUri(doc.uri, MediaType.DOCUMENT, doc.mimeType, doc.displayName) },
                                onToggleFavorite = { onToggleFavorite(doc.uri, MediaType.DOCUMENT, doc.displayName, doc.mimeType) },
                                onShare = { onShareUri(doc.uri, doc.mimeType) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (results.favorites.isNotEmpty()) {
                        item(key = "cat_favs_header") {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeaderRow(title = "Favorites", count = results.favorites.size, actionLabel = null)
                        }
                        items(results.favorites, key = { "fav_${it.uri}" }) { fav ->
                            val synthetic = MediaItem(
                                uri = fav.uri,
                                displayName = fav.displayName,
                                mediaType = fav.mediaType,
                                mimeType = fav.mimeType,
                                dateModified = fav.addedAt
                            )
                            DocumentRowCard(
                                item = synthetic,
                                isFavorite = true,
                                onOpen = {
                                    when (fav.mediaType) {
                                        MediaType.PHOTO -> onOpenPhoto(synthetic)
                                        MediaType.VIDEO -> onOpenVideo(synthetic)
                                        else -> onOpenDocumentUri(fav.uri, fav.mediaType, fav.mimeType, fav.displayName)
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(fav.uri, fav.mediaType, fav.displayName, fav.mimeType) },
                                onShare = { onShareUri(fav.uri, fav.mimeType) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (results.recents.isNotEmpty()) {
                        item(key = "cat_recents_header") {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeaderRow(title = "Recent Items", count = results.recents.size, actionLabel = null)
                        }
                        items(results.recents, key = { "rec_${it.uri}" }) { rec ->
                            val synthetic = MediaItem(
                                uri = rec.uri,
                                displayName = rec.displayName,
                                mediaType = rec.mediaType,
                                mimeType = rec.mimeType,
                                dateModified = rec.lastAccessedAt
                            )
                            DocumentRowCard(
                                item = synthetic,
                                isFavorite = rec.uri in favoriteUris,
                                onOpen = {
                                    when (rec.mediaType) {
                                        MediaType.PHOTO -> onOpenPhoto(synthetic)
                                        MediaType.VIDEO -> onOpenVideo(synthetic)
                                        else -> onOpenDocumentUri(rec.uri, rec.mediaType, rec.mimeType, rec.displayName)
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(rec.uri, rec.mediaType, rec.displayName, rec.mimeType) },
                                onShare = { onShareUri(rec.uri, rec.mimeType) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
