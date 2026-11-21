package com.example.vaultgallery.ui.viewer

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.vaultgallery.data.db.Bookmark
import com.example.vaultgallery.pdf.PdfDocumentEngine
import com.example.vaultgallery.pdf.PdfOpenResult
import com.example.vaultgallery.pdf.PdfSearchMatch
import com.example.vaultgallery.util.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdfUri: String,
    displayName: String,
    initialPage: Int,
    isFavorite: Boolean,
    bookmarks: List<Bookmark>,
    onBack: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onDeleteBookmark: (Long) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val engine = remember(pdfUri) { PdfDocumentEngine(context) }

    var openState by remember(pdfUri) { mutableStateOf<PdfOpenResult?>(null) }
    var currentPage by remember(pdfUri) { mutableIntStateOf(initialPage.coerceAtLeast(0)) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPage by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var fitWidth by rememberSaveable { mutableStateOf(true) }
    var rotationDegrees by rememberSaveable { mutableIntStateOf(0) }
    var fullScreen by remember { mutableStateOf(false) }
    var showThumbnails by rememberSaveable { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PdfSearchMatch>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Immersive system bars for true fullscreen PDF reading
    DisposableEffect(fullScreen) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (fullScreen) {
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

    DisposableEffect(engine) {
        onDispose { engine.close() }
    }

    LaunchedEffect(pdfUri) {
        val result = engine.open(Uri.parse(pdfUri))
        openState = result
        if (result is PdfOpenResult.Success) {
            currentPage = initialPage.coerceIn(0, (result.pageCount - 1).coerceAtLeast(0))
            onPageChanged(currentPage)
        }
    }

    val totalPages = (openState as? PdfOpenResult.Success)?.pageCount ?: 0
    val isBookmarked = bookmarks.any { it.pageIndex == currentPage }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("pdf_reader_root")
    ) {
        val density = LocalDensity.current
        val targetWidthPx = remember(maxWidth, zoomScale) {
            with(density) { (maxWidth.toPx() * zoomScale.coerceIn(1f, 2f)).toInt().coerceIn(540, 2000) }
        }

        LaunchedEffect(openState, currentPage, targetWidthPx, rotationDegrees) {
            if (openState is PdfOpenResult.Success) {
                isRenderingPage = true
                pageBitmap = engine.renderPage(
                    pageIndex = currentPage,
                    targetWidthPx = targetWidthPx,
                    rotationDegrees = rotationDegrees
                )
                isRenderingPage = false
                onPageChanged(currentPage)
            }
        }

        // Main PDF Viewport or Error State
        when (val state = openState) {
            null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Opening PDF…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            PdfOpenResult.PasswordProtected -> {
                PdfErrorStateView(
                    icon = Icons.Filled.Lock,
                    title = "Password-Protected PDF",
                    body = "This PDF is encrypted and requires a password. Android's built-in PdfRenderer cannot unlock password-protected PDFs directly. You can open it in an external PDF reader.",
                    primaryLabel = "Open Externally",
                    onPrimary = { FormatUtils.openExternalViewer(context, pdfUri, "application/pdf") },
                    onBack = onBack
                )
            }

            is PdfOpenResult.CorruptedOrUnsupported -> {
                PdfErrorStateView(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Unable to Render PDF",
                    body = state.reason,
                    primaryLabel = "Try External Viewer",
                    onPrimary = { FormatUtils.openExternalViewer(context, pdfUri, "application/pdf") },
                    onBack = onBack
                )
            }

            is PdfOpenResult.Inaccessible -> {
                PdfErrorStateView(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Document Unavailable",
                    body = state.reason,
                    primaryLabel = null,
                    onPrimary = null,
                    onBack = onBack
                )
            }

            is PdfOpenResult.Success -> {
                val scrollState = rememberScrollState()
                LaunchedEffect(currentPage, fitWidth, rotationDegrees) {
                    scrollState.scrollTo(0)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF151820))
                        .pointerInput(currentPage) {
                            detectTapGestures(
                                onTap = { fullScreen = !fullScreen },
                                onDoubleTap = {
                                    if (zoomScale > 1.2f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.2f
                                    }
                                }
                            )
                        }
                        .pointerInput(currentPage) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pointerCount = event.changes.count { it.pressed }
                                    if (pointerCount >= 2 || zoomScale > 1.02f) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val updatedScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
                                        zoomScale = updatedScale
                                        if (updatedScale > 1.02f) {
                                            val maxX = size.width * (updatedScale - 1f) / 2f
                                            val maxY = size.height * (updatedScale - 1f) / 2f
                                            panOffset = Offset(
                                                x = (panOffset.x + panChange.x).coerceIn(-maxX, maxX),
                                                y = (panOffset.y + panChange.y).coerceIn(-maxY, maxY)
                                            )
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    pageBitmap?.let { bmp ->
                        val pageAspect = (bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat())
                            .coerceIn(0.25f, 4.0f)

                        if (fitWidth && zoomScale <= 1.02f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(
                                        top = if (fullScreen) 8.dp else 76.dp,
                                        bottom = if (fullScreen) 8.dp else 132.dp,
                                        start = if (fullScreen) 0.dp else 8.dp,
                                        end = if (fullScreen) 0.dp else 8.dp
                                    ),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "PDF page ${currentPage + 1} of $totalPages",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(pageAspect)
                                        .background(Color.White)
                                )
                            }
                        } else {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF page ${currentPage + 1} of $totalPages",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = if (fullScreen) 0.dp else 8.dp)
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale,
                                        translationX = panOffset.x,
                                        translationY = panOffset.y
                                    )
                            )
                        }
                    }

                    if (isRenderingPage) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                        )
                    }
                }
            }
        }

        // Top Toolbar (when not fullscreen)
        AnimatedVisibility(
            visible = !fullScreen && openState is PdfOpenResult.Success,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pdf_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ${currentPage + 1} of $totalPages • ${if (fitWidth) "Fit Width" else "Fit Page"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showSearchSheet = true }, modifier = Modifier.testTag("pdf_search_button")) {
                        Icon(Icons.Filled.Search, contentDescription = "Search PDF")
                    }
                    IconButton(onClick = { onToggleBookmark(currentPage) }, modifier = Modifier.testTag("pdf_bookmark_button")) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark page",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("pdf_favorite_button")) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite PDF",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { FormatUtils.shareUri(context, pdfUri, "application/pdf", "Share PDF") },
                        modifier = Modifier.testTag("pdf_share_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share PDF")
                    }
                }
            }
        }

        // Bottom Controls & Optional Thumbnail Strip
        AnimatedVisibility(
            visible = !fullScreen && openState is PdfOpenResult.Success,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (showThumbnails && totalPages > 0) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(totalPages, key = { it }) { pageIdx ->
                                PdfThumbnailTile(
                                    engine = engine,
                                    pageIndex = pageIdx,
                                    isSelected = pageIdx == currentPage,
                                    onClick = {
                                        currentPage = pageIdx
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    }
                                )
                            }
                        }
                    }

                    if (totalPages > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentPage > 0) {
                                        currentPage--
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    }
                                },
                                enabled = currentPage > 0,
                                modifier = Modifier.testTag("pdf_prev_page")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
                            }
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { currentPage = it.toInt().coerceIn(0, totalPages - 1) },
                                valueRange = 0f..(totalPages - 1).toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (currentPage < totalPages - 1) {
                                        currentPage++
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    }
                                },
                                enabled = currentPage < totalPages - 1,
                                modifier = Modifier.testTag("pdf_next_page")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showThumbnails = !showThumbnails }) {
                            Icon(
                                Icons.Filled.ViewCarousel,
                                contentDescription = "Page thumbnails",
                                tint = if (showThumbnails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            fitWidth = !fitWidth
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }) {
                            Icon(
                                Icons.Filled.FitScreen,
                                contentDescription = if (fitWidth) "Switch to Fit Page" else "Switch to Fit Width",
                                tint = if (fitWidth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { rotationDegrees = (rotationDegrees + 90) % 360 }) {
                            Icon(Icons.Filled.Rotate90DegreesCw, contentDescription = "Rotate 90 degrees")
                        }
                        IconButton(onClick = {
                            zoomScale = (zoomScale - 0.35f).coerceAtLeast(1f)
                            if (zoomScale <= 1.02f) panOffset = Offset.Zero
                        }) {
                            Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out")
                        }
                        IconButton(onClick = { zoomScale = (zoomScale + 0.35f).coerceAtMost(4f) }) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in")
                        }
                        IconButton(onClick = { showBookmarksSheet = true }) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = "Bookmarks list")
                        }
                        IconButton(onClick = { fullScreen = !fullScreen }) {
                            Icon(
                                if (fullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Toggle full screen"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text("Bookmarks (${bookmarks.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                if (bookmarks.isEmpty()) {
                    Text(
                        "Tap the bookmark icon on any page to save quick jumps here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bm ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPage = bm.pageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                                        showBookmarksSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(bm.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Page ${bm.pageIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove bookmark")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearchSheet) {
        ModalBottomSheet(onDismissRequest = { showSearchSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text("Search / Jump in PDF", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Enter text or page number (1–$totalPages)") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isSearching = true
                                searchResults = engine.searchInPdf(searchQuery)
                                isSearching = false
                            }
                        }
                    ) {
                        Text("Find")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                } else if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(220.dp)
                    ) {
                        items(searchResults) { match ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPage = match.pageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                                        showSearchSheet = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Page ${match.pageIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        match.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotBlank()) {
                    Text(
                        "No direct matches found. Tip: enter a page number (1–$totalPages) to jump directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PdfThumbnailTile(
    engine: PdfDocumentEngine,
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var thumb by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        thumb = engine.renderThumbnail(pageIndex)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(width = 64.dp, height = 88.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            thumb?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1} thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(3.dp)
            ) {
                Text(
                    text = "${pageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun PdfErrorStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    primaryLabel: String?,
    onPrimary: (() -> Unit)?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (primaryLabel != null && onPrimary != null) {
                Button(onClick = onPrimary) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(primaryLabel)
                }
            }
            OutlinedButton(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}
