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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.vaultgallery.data.db.RecentItem
import com.example.vaultgallery.ui.components.DocumentRowCard
import com.example.vaultgallery.ui.components.EmptyVaultState
import com.example.vaultgallery.ui.components.SectionHeaderRow

@Composable
fun DocumentsScreen(
    documents: List<MediaItem>,
    recentDocuments: List<RecentItem>,
    favoriteUris: Set<String>,
    onPickDocuments: () -> Unit,
    onLinkFolder: () -> Unit,
    onOpenDocument: (MediaItem) -> Unit,
    onOpenRecentDoc: (RecentItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onShareDocument: (MediaItem) -> Unit,
    onRenameDocument: (MediaItem, String) -> Unit,
    onDeleteDocument: (MediaItem) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var renameTarget by remember { mutableStateOf<MediaItem?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<MediaItem?>(null) }

    val filteredDocs = remember(documents, selectedFilter) {
        when (selectedFilter) {
            "PDF" -> documents.filter { it.mediaType == MediaType.PDF }
            "DOCS" -> documents.filter { it.mediaType == MediaType.DOCUMENT }
            else -> documents
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPickDocuments,
                modifier = Modifier
                    .weight(1f)
                    .testTag("documents_open_saf_button")
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Document")
            }
            OutlinedButton(
                onClick = onLinkFolder,
                modifier = Modifier
                    .weight(1f)
                    .testTag("documents_link_folder_button")
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Link Folder")
            }
        }

        // Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            val filters = listOf(
                "ALL" to "All (${documents.size})",
                "PDF" to "PDFs (${documents.count { it.mediaType == MediaType.PDF }})",
                "DOCS" to "Other Docs (${documents.count { it.mediaType == MediaType.DOCUMENT }})",
                "RECENT" to "Recent (${recentDocuments.size})"
            )
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label) },
                    modifier = Modifier.testTag("docs_filter_$key")
                )
            }
        }

        if (selectedFilter == "RECENT") {
            if (recentDocuments.isEmpty()) {
                EmptyVaultState(
                    title = "No Recent Documents",
                    body = "PDFs and documents you open appear here with your last reading page remembered.",
                    primaryActionLabel = "Open Document",
                    onPrimaryAction = onPickDocuments
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recentDocuments, key = { it.uri }) { recent ->
                        val synthetic = MediaItem(
                            uri = recent.uri,
                            displayName = recent.displayName,
                            mediaType = recent.mediaType,
                            mimeType = recent.mimeType,
                            dateModified = recent.lastAccessedAt
                        )
                        DocumentRowCard(
                            item = synthetic,
                            isFavorite = recent.uri in favoriteUris,
                            onOpen = { onOpenRecentDoc(recent) },
                            onToggleFavorite = { onToggleFavorite(synthetic) },
                            onShare = { onShareDocument(synthetic) }
                        )
                    }
                }
            }
        } else if (filteredDocs.isEmpty()) {
            EmptyVaultState(
                title = "No Documents Linked",
                body = "Browse PDFs, Word docs, spreadsheets, and text files securely with Android's Storage Access Framework.",
                primaryActionLabel = "Open PDF / Doc",
                onPrimaryAction = onPickDocuments,
                secondaryActionLabel = "Link Folder",
                onSecondaryAction = onLinkFolder
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("documents_list")
            ) {
                if (recentDocuments.isNotEmpty() && selectedFilter == "ALL") {
                    item(key = "recent_header") {
                        SectionHeaderRow(
                            title = "Recently Opened",
                            count = recentDocuments.size,
                            actionLabel = null
                        )
                    }
                }
                items(filteredDocs, key = { it.uri }) { doc ->
                    DocumentRowCard(
                        item = doc,
                        isFavorite = doc.uri in favoriteUris,
                        onOpen = { onOpenDocument(doc) },
                        onToggleFavorite = { onToggleFavorite(doc) },
                        onShare = { onShareDocument(doc) },
                        onRename = {
                            renameTarget = doc
                            renameInput = doc.displayName
                        },
                        onDelete = { deleteTarget = doc }
                    )
                }
            }
        }
    }

    // Rename Dialog
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Document") },
            text = {
                Column {
                    Text(
                        text = "Enter a new filename (supported on compatible Storage Access Framework providers).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        label = { Text("Filename") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_document_input")
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val trimmed = renameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenameDocument(target, trimmed)
                        }
                        renameTarget = null
                    },
                    modifier = Modifier.testTag("rename_document_confirm")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Document?") },
            text = {
                Text(
                    "Remove \"${target.displayName}\"? If permitted by Android's storage provider, the document will be permanently deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDocument(target)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("delete_document_confirm")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}
