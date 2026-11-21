package com.example.vaultgallery.ui.viewer

import android.app.Activity
import android.app.RecoverableSecurityException
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaSource
import com.example.vaultgallery.util.FormatUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photos: List<MediaItem>,
    initialUri: String,
    favoriteUris: Set<String>,
    slideshowIntervalSec: Int,
    slideshowLoop: Boolean,
    onBack: () -> Unit,
    onRecordRecent: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onDeleteMediaStoreConfirmed: (String) -> Unit,
    onDeleteSafItem: (MediaItem) -> Unit,
    onRemoveMissing: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var deletedUris by remember { mutableStateOf(setOf<String>()) }
    val safeList = remember(photos, initialUri, deletedUris) {
        val active = photos.filterNot { it.uri in deletedUris }
        if (active.any { it.uri == initialUri } || initialUri in deletedUris) {
            active
        } else {
            listOf(
                MediaItem(
                    uri = initialUri,
                    displayName = Uri.parse(initialUri).lastPathSegment ?: "Photo",
                    mediaType = "PHOTO",
                    mimeType = "image/*"
                )
            ) + active
        }
    }

    LaunchedEffect(safeList.size) {
        if (safeList.isEmpty()) onBack()
    }

    val initialIndex = remember(initialUri) {
        safeList.indexOfFirst { it.uri == initialUri }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { safeList.size }
    var chromeVisible by remember { mutableStateOf(true) }
    var isZoomed by remember { mutableStateOf(false) }
    var slideshowActive by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var confirmSafDeleteTarget by remember { mutableStateOf<MediaItem?>(null) }
    var pendingMediaStoreDeleteUri by remember { mutableStateOf<String?>(null) }

    val currentPhoto = safeList.getOrNull(pagerState.currentPage)

    // Immersive bars toggle
    DisposableEffect(chromeVisible) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (!chromeVisible) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = (context as? Activity)?.window ?: return@onDispose
            WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // System user-confirmation delete launcher for Android 10 (RecoverableSecurityException) & Android 11+ (createDeleteRequest)
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val deletedUri = pendingMediaStoreDeleteUri
        pendingMediaStoreDeleteUri = null
        if (result.resultCode == Activity.RESULT_OK && deletedUri != null) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                runCatching { context.contentResolver.delete(Uri.parse(deletedUri), null, null) }
            }
            deletedUris = deletedUris + deletedUri
            onDeleteMediaStoreConfirmed(deletedUri)
            if (safeList.size <= 1) onBack()
        } else {
            onShowMessage("Delete canceled")
        }
    }

    LaunchedEffect(currentPhoto?.uri) {
        currentPhoto?.let(onRecordRecent)
    }

    LaunchedEffect(slideshowActive, pagerState.currentPage, safeList.size, slideshowIntervalSec, slideshowLoop) {
        if (!slideshowActive || safeList.size <= 1) return@LaunchedEffect
        delay(slideshowIntervalSec * 1000L)
        val next = pagerState.currentPage + 1
        if (next < safeList.size) {
            pagerState.animateScrollToPage(next)
        } else if (slideshowLoop) {
            pagerState.animateScrollToPage(0)
        } else {
            slideshowActive = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("photo_viewer_root")
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = safeList[page]
            ZoomablePhotoPage(
                uri = photo.uri,
                contentDescription = photo.displayName,
                isCurrentPage = page == pagerState.currentPage,
                onZoomChanged = { zoomed ->
                    if (page == pagerState.currentPage) isZoomed = zoomed
                },
                onTap = { chromeVisible = !chromeVisible },
                onRemoveMissing = {
                    deletedUris = deletedUris + photo.uri
                    onRemoveMissing(photo.uri)
                    if (safeList.size <= 1) onBack()
                }
            )
        }

        // Top Bar Overlay
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("photo_viewer_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPhoto?.displayName.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} / ${safeList.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                IconButton(
                    onClick = { slideshowActive = !slideshowActive },
                    modifier = Modifier.testTag("photo_viewer_slideshow")
                ) {
                    Icon(
                        imageVector = if (slideshowActive) Icons.Filled.Pause else Icons.Filled.Slideshow,
                        contentDescription = if (slideshowActive) "Stop slideshow" else "Start slideshow",
                        tint = if (slideshowActive) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(
                    onClick = { showInfoSheet = true },
                    modifier = Modifier.testTag("photo_viewer_info")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Photo details",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Actions Overlay
        AnimatedVisibility(
            visible = chromeVisible && currentPhoto != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            currentPhoto?.let { photo ->
                val isFav = photo.uri in favoriteUris
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleFavorite(photo) },
                        modifier = Modifier.testTag("photo_viewer_favorite")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFav) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            FormatUtils.shareUri(context, photo.uri, photo.mimeType, "Share Photo")
                        },
                        modifier = Modifier.testTag("photo_viewer_share")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share photo",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            if (photo.source == MediaSource.MEDIASTORE) {
                                when {
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                                        runCatching {
                                            val deleteIntent = MediaStore.createDeleteRequest(
                                                context.contentResolver,
                                                listOf(Uri.parse(photo.uri))
                                            )
                                            pendingMediaStoreDeleteUri = photo.uri
                                            deleteRequestLauncher.launch(
                                                IntentSenderRequest.Builder(deleteIntent.intentSender).build()
                                            )
                                        }.onFailure {
                                            onShowMessage("Android could not open delete confirmation for this item.")
                                        }
                                    }
                                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                                        try {
                                            val rows = context.contentResolver.delete(Uri.parse(photo.uri), null, null)
                                            if (rows > 0) {
                                                deletedUris = deletedUris + photo.uri
                                                onDeleteMediaStoreConfirmed(photo.uri)
                                                if (safeList.size <= 1) onBack()
                                            }
                                        } catch (recoverable: RecoverableSecurityException) {
                                            pendingMediaStoreDeleteUri = photo.uri
                                            deleteRequestLauncher.launch(
                                                IntentSenderRequest.Builder(
                                                    recoverable.userAction.actionIntent.intentSender
                                                ).build()
                                            )
                                        } catch (_: Exception) {
                                            onShowMessage("Unable to delete this photo.")
                                        }
                                    }
                                    else -> {
                                        confirmSafDeleteTarget = photo
                                    }
                                }
                            } else {
                                confirmSafDeleteTarget = photo
                            }
                        },
                        modifier = Modifier.testTag("photo_viewer_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete photo",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showInfoSheet && currentPhoto != null) {
        ModalBottomSheet(onDismissRequest = { showInfoSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Photo Details",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                DetailInfoRow("Filename", currentPhoto.displayName)
                DetailInfoRow("Album", currentPhoto.bucketName.ifBlank { "Gallery" })
                DetailInfoRow("Date", FormatUtils.formatDateTime(currentPhoto.dateModified))
                DetailInfoRow("Size", FormatUtils.formatFileSize(currentPhoto.sizeBytes))
                if (currentPhoto.width > 0 && currentPhoto.height > 0) {
                    DetailInfoRow("Resolution", "${currentPhoto.width} × ${currentPhoto.height}")
                }
                DetailInfoRow("Format", currentPhoto.mimeType.ifBlank { "Image" })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    confirmSafDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmSafDeleteTarget = null },
            title = { Text("Remove Photo?") },
            text = {
                Text("Remove \"${target.displayName}\"? On SAF providers this deletes the file; on picked media it removes it from Vault Gallery.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletedUris = deletedUris + target.uri
                        onDeleteSafItem(target)
                        confirmSafDeleteTarget = null
                        if (safeList.size <= 1) onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSafDeleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ZoomablePhotoPage(
    uri: String,
    contentDescription: String,
    isCurrentPage: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
    onRemoveMissing: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && scale != 1f) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.02f)
    }

    val imageRequest = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.6f
                        }
                    }
                )
            }
            .pointerInput(uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }
                        if (pointerCount >= 2 || scale > 1.02f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                            if (newScale > 1.02f) {
                                val maxX = (size.width * (newScale - 1f)) / 2f
                                val maxY = (size.height * (newScale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                )
                                scale = newScale
                                event.changes.forEach { it.consume() }
                            } else {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            error = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = "Image unavailable",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Unable to load photo. File may have been deleted or access revoked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(onClick = onRemoveMissing) {
                        Text("Remove Missing Item", color = Color.White)
                    }
                }
            }
        )
    }
}
