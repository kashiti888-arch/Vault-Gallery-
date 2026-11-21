package com.example.vaultgallery.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.data.media.MediaPermissionManager
import com.example.vaultgallery.security.BiometricStatus
import com.example.vaultgallery.ui.navigation.MainDestination
import com.example.vaultgallery.ui.navigation.VaultRoutes
import com.example.vaultgallery.ui.screens.AlbumDetailScreen
import com.example.vaultgallery.ui.screens.AlbumsScreen
import com.example.vaultgallery.ui.screens.DocumentsScreen
import com.example.vaultgallery.ui.screens.FavoritesScreen
import com.example.vaultgallery.ui.screens.HomeScreen
import com.example.vaultgallery.ui.screens.PhotosScreen
import com.example.vaultgallery.ui.screens.SearchScreen
import com.example.vaultgallery.ui.screens.SettingsScreen
import com.example.vaultgallery.ui.screens.VideosScreen
import com.example.vaultgallery.ui.security.AppLockOverlay
import com.example.vaultgallery.ui.viewer.PdfReaderScreen
import com.example.vaultgallery.ui.viewer.PhotoViewerScreen
import com.example.vaultgallery.ui.viewer.VideoPlayerScreen
import com.example.vaultgallery.util.FormatUtils
import com.example.vaultgallery.viewmodel.VaultGalleryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultGalleryApp(
    viewModel: VaultGalleryViewModel,
    activity: FragmentActivity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accessState by viewModel.mediaAccessState.collectAsStateWithLifecycle()
    val permissionRequestedOnce by viewModel.mediaPermissionRequestedOnce.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val documents by viewModel.documentsAndPdfs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteUris by viewModel.favoriteUris.collectAsStateWithLifecycle()
    val recentDocuments by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val isLockStateResolved by viewModel.isLockStateResolved.collectAsStateWithLifecycle()
    val hasPinConfigured by viewModel.hasPinConfigured.collectAsStateWithLifecycle()
    val failedAttempts by viewModel.failedAttempts.collectAsStateWithLifecycle()
    val lockoutUntilMs by viewModel.lockoutUntilMs.collectAsStateWithLifecycle()
    val keystoreRecoveryNeeded by viewModel.keystoreRecoveryNeeded.collectAsStateWithLifecycle()
    val authorizedBridgeStatus by viewModel.authorizedBridgeStatus.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    var biometricStatus by remember(activity) {
        mutableStateOf(viewModel.appLockManager.checkBiometricStatus(activity))
    }
    var deviceCredentialAvailable by remember(activity) {
        mutableStateOf(viewModel.appLockManager.isDeviceCredentialAvailable(activity))
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                biometricStatus = viewModel.appLockManager.checkBiometricStatus(activity)
                deviceCredentialAvailable = viewModel.appLockManager.isDeviceCredentialAvailable(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var appliedInitialDefault by remember { mutableStateOf(false) }
    LaunchedEffect(isLockStateResolved) {
        if (isLockStateResolved && !appliedInitialDefault) {
            appliedInitialDefault = true
            val persistedDefault = viewModel.getDefaultSectionOnce()
            val dest = MainDestination.fromSettingKey(persistedDefault)
            if (dest != MainDestination.HOME) {
                navigateToMainDestination(navController, dest)
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Modern Android Permission & Picker Launchers
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.markMediaPermissionRequested()
        viewModel.refreshPermissionsAndSync(force = true)
    }

    val pickAllVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        viewModel.importPickedPhotos(uris)
    }

    val pickPhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        viewModel.importPickedPhotos(uris)
    }

    val pickVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(25)
    ) { uris ->
        viewModel.importPickedVideos(uris)
    }

    val openDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.importSafDocuments(uris) { pdfUri ->
            navController.navigate(VaultRoutes.pdfReader(pdfUri))
        }
    }

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        viewModel.importSafFolder(treeUri)
    }

    // Smart permission request that handles first launch, partial access, denied, and permanently denied
    val requestMediaPermissions: () -> Unit = {
        val perms = MediaPermissionManager.requiredMediaPermissions()
        val anyRationale = perms.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        if (accessState == MediaAccessState.DENIED && permissionRequestedOnce && !anyRationale) {
            viewModel.showMessage("Enable Photos & Videos permission in Android App Settings")
            FormatUtils.openAppSystemSettings(context)
        } else {
            mediaPermissionLauncher.launch(perms)
        }
    }

    val launchPickAllMedia: () -> Unit = {
        pickAllVisualMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }
    val launchPickPhotos: () -> Unit = {
        pickPhotosLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val launchPickVideos: () -> Unit = {
        pickVideosLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }
    val launchPickDocuments: () -> Unit = {
        openDocumentsLauncher.launch(
            arrayOf(
                "application/pdf",
                "text/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "*/*"
            )
        )
    }
    val launchLinkFolder: () -> Unit = {
        openFolderLauncher.launch(null)
    }

    val openDocumentAction: (String, String, String, String) -> Unit = { uri, mediaType, mimeType, name ->
        viewModel.recordRecent(uri, mediaType, name, mimeType)
        if (mediaType == MediaType.PDF || mimeType.equals("application/pdf", ignoreCase = true) ||
            name.endsWith(".pdf", ignoreCase = true)
        ) {
            navController.navigate(VaultRoutes.pdfReader(uri))
        } else {
            val opened = FormatUtils.openExternalViewer(context, uri, mimeType)
            if (!opened) {
                viewModel.showMessage("No installed app can open this document type.")
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainDestination.HOME.route
    val activeMainDestination = MainDestination.entries.firstOrNull { it.route == currentRoute }
    val showMainChrome = activeMainDestination != null

    Box(modifier = Modifier.fillMaxSize()) {
        // Prevent any flash of private media before cold-start App Lock state resolves
        if (!isLockStateResolved) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Loading Vault Gallery",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            return@Box
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 700.dp

            val contentScaffold: @Composable () -> Unit = {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideScreen && showMainChrome) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MainDestination.entries.forEach { dest ->
                                val selected = activeMainDestination == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToMainDestination(navController, dest) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("rail_${dest.route}")
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            if (showMainChrome) {
                                Column {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = if (activeMainDestination == MainDestination.HOME) {
                                                    "Vault Gallery"
                                                } else {
                                                    activeMainDestination.label
                                                },
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        },
                                        navigationIcon = {
                                            if (!isWideScreen) {
                                                IconButton(
                                                    onClick = { scope.launch { drawerState.open() } },
                                                    modifier = Modifier.testTag("open_nav_drawer")
                                                ) {
                                                    Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                                                }
                                            }
                                        },
                                        actions = {
                                            IconButton(
                                                onClick = { navController.navigate(VaultRoutes.SEARCH) },
                                                modifier = Modifier.testTag("topbar_search_button")
                                            ) {
                                                Icon(Icons.Filled.Search, contentDescription = "Search")
                                            }
                                            if (!isWideScreen) {
                                                val favSelected = activeMainDestination == MainDestination.FAVORITES
                                                IconButton(
                                                    onClick = {
                                                        navigateToMainDestination(navController, MainDestination.FAVORITES)
                                                    },
                                                    modifier = Modifier.testTag("topbar_favorites_button")
                                                ) {
                                                    Icon(
                                                        imageVector = if (favSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                        contentDescription = "Favorites",
                                                        tint = if (favSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                val settingsSelected = activeMainDestination == MainDestination.SETTINGS
                                                IconButton(
                                                    onClick = {
                                                        navigateToMainDestination(navController, MainDestination.SETTINGS)
                                                    },
                                                    modifier = Modifier.testTag("topbar_settings_button")
                                                ) {
                                                    Icon(
                                                        imageVector = if (settingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                        contentDescription = "Settings",
                                                        tint = if (settingsSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                    if (isSyncing) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .height(2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (!isWideScreen && showMainChrome) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    MainDestination.entries
                                        .filter { it.showInBottomBar }
                                        .forEach { dest ->
                                            val selected = activeMainDestination == dest
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateToMainDestination(navController, dest) },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = dest.label
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = dest.label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                modifier = Modifier.testTag("bottom_nav_${dest.route}")
                                            )
                                        }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = MainDestination.HOME.route,
                            modifier = if (showMainChrome) {
                                Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                            // 1. HOME
                            composable(MainDestination.HOME.route) {
                                HomeScreen(
                                    photos = photos,
                                    videos = videos,
                                    documents = documents,
                                    recentDocuments = recentDocuments,
                                    favorites = favorites,
                                    favoriteUris = favoriteUris,
                                    accessState = accessState,
                                    isSyncing = isSyncing,
                                    onOpenSearch = { navController.navigate(VaultRoutes.SEARCH) },
                                    onRequestMediaPermission = requestMediaPermissions,
                                    onPickMedia = launchPickAllMedia,
                                    onPickDocuments = launchPickDocuments,
                                    onLinkFolder = launchLinkFolder,
                                    onOpenSystemSettings = { FormatUtils.openAppSystemSettings(context) },
                                    onSeeAllPhotos = { navigateToMainDestination(navController, MainDestination.PHOTOS) },
                                    onSeeAllVideos = { navigateToMainDestination(navController, MainDestination.VIDEOS) },
                                    onSeeAllDocuments = { navigateToMainDestination(navController, MainDestination.DOCUMENTS) },
                                    onSeeAllFavorites = { navigateToMainDestination(navController, MainDestination.FAVORITES) },
                                    onOpenPhoto = { photo ->
                                        navController.navigate(VaultRoutes.photoViewer(photo.uri, "ALL"))
                                    },
                                    onOpenVideo = { video ->
                                        navController.navigate(VaultRoutes.videoPlayer(video.uri))
                                    },
                                    onOpenDocumentUri = openDocumentAction
                                )
                            }

                            // 2. PHOTOS
                            composable(MainDestination.PHOTOS.route) {
                                PhotosScreen(
                                    photos = photos,
                                    favoriteUris = favoriteUris,
                                    gridColumns = settings.gridColumns,
                                    sortOrder = settings.sortOrder,
                                    accessState = accessState,
                                    onSelectSort = viewModel::setSortOrder,
                                    onRefresh = { viewModel.refreshPermissionsAndSync(force = true) },
                                    onRequestMediaPermission = requestMediaPermissions,
                                    onPickPhotos = launchPickPhotos,
                                    onOpenSystemSettings = { FormatUtils.openAppSystemSettings(context) },
                                    onOpenPhoto = { photo ->
                                        navController.navigate(VaultRoutes.photoViewer(photo.uri, "ALL"))
                                    }
                                )
                            }

                            // 3. ALBUMS
                            composable(MainDestination.ALBUMS.route) {
                                AlbumsScreen(
                                    albums = albums,
                                    accessState = accessState,
                                    onRequestMediaPermission = requestMediaPermissions,
                                    onPickMedia = launchPickAllMedia,
                                    onLinkFolder = launchLinkFolder,
                                    onOpenSystemSettings = { FormatUtils.openAppSystemSettings(context) },
                                    onOpenAlbum = { album ->
                                        navController.navigate(VaultRoutes.albumDetail(album.bucketId, album.bucketName))
                                    }
                                )
                            }

                            // ALBUM DETAIL
                            composable(
                                route = VaultRoutes.ALBUM_DETAIL,
                                arguments = listOf(
                                    navArgument("bucketId") { type = NavType.StringType },
                                    navArgument("bucketName") { type = NavType.StringType }
                                )
                            ) { backStack ->
                                val bucketId = VaultRoutes.decodeArg(backStack.arguments?.getString("bucketId"))
                                val bucketName = VaultRoutes.decodeArg(backStack.arguments?.getString("bucketName"))
                                val albumItems by remember(bucketId) {
                                    viewModel.observeAlbumItems(bucketId)
                                }.collectAsStateWithLifecycle(initialValue = emptyList())

                                AlbumDetailScreen(
                                    bucketName = bucketName.ifBlank { "Album" },
                                    items = albumItems,
                                    favoriteUris = favoriteUris,
                                    gridColumns = settings.gridColumns,
                                    onBack = { navController.popBackStack() },
                                    onOpenPhoto = { photo ->
                                        navController.navigate(VaultRoutes.photoViewer(photo.uri, bucketId))
                                    },
                                    onOpenVideo = { video ->
                                        navController.navigate(VaultRoutes.videoPlayer(video.uri))
                                    },
                                    onOpenDocument = { doc ->
                                        openDocumentAction(doc.uri, doc.mediaType, doc.mimeType, doc.displayName)
                                    }
                                )
                            }

                            // 4. VIDEOS
                            composable(MainDestination.VIDEOS.route) {
                                VideosScreen(
                                    videos = videos,
                                    favoriteUris = favoriteUris,
                                    sortOrder = settings.sortOrder,
                                    accessState = accessState,
                                    onSelectSort = viewModel::setSortOrder,
                                    onRequestMediaPermission = requestMediaPermissions,
                                    onPickVideos = launchPickVideos,
                                    onOpenSystemSettings = { FormatUtils.openAppSystemSettings(context) },
                                    onOpenVideo = { video ->
                                        navController.navigate(VaultRoutes.videoPlayer(video.uri))
                                    },
                                    onShareVideo = { video ->
                                        FormatUtils.shareUri(context, video.uri, video.mimeType, "Share Video")
                                    }
                                )
                            }

                            // 5. DOCUMENTS
                            composable(MainDestination.DOCUMENTS.route) {
                                DocumentsScreen(
                                    documents = documents,
                                    recentDocuments = recentDocuments,
                                    favoriteUris = favoriteUris,
                                    onPickDocuments = launchPickDocuments,
                                    onLinkFolder = launchLinkFolder,
                                    onOpenDocument = { doc ->
                                        openDocumentAction(doc.uri, doc.mediaType, doc.mimeType, doc.displayName)
                                    },
                                    onOpenRecentDoc = { recent ->
                                        openDocumentAction(recent.uri, recent.mediaType, recent.mimeType, recent.displayName)
                                    },
                                    onToggleFavorite = { doc ->
                                        viewModel.toggleFavorite(doc.uri, doc.mediaType, doc.displayName, doc.mimeType)
                                    },
                                    onShareDocument = { doc ->
                                        FormatUtils.shareUri(context, doc.uri, doc.mimeType, "Share Document")
                                    },
                                    onRenameDocument = viewModel::renameDocument,
                                    onDeleteDocument = viewModel::deleteSafDocument
                                )
                            }

                            // 6. FAVORITES
                            composable(MainDestination.FAVORITES.route) {
                                FavoritesScreen(
                                    favorites = favorites,
                                    gridColumns = settings.gridColumns,
                                    onOpenPhoto = { fav ->
                                        navController.navigate(VaultRoutes.photoViewer(fav.uri, "ALL"))
                                    },
                                    onOpenVideo = { fav ->
                                        navController.navigate(VaultRoutes.videoPlayer(fav.uri))
                                    },
                                    onOpenDocument = { fav ->
                                        openDocumentAction(fav.uri, fav.mediaType, fav.mimeType, fav.displayName)
                                    },
                                    onToggleFavorite = { fav ->
                                        viewModel.toggleFavorite(fav.uri, fav.mediaType, fav.displayName, fav.mimeType)
                                    },
                                    onShare = { fav ->
                                        FormatUtils.shareUri(context, fav.uri, fav.mimeType, "Share")
                                    }
                                )
                            }

                            // 7. SETTINGS
                            composable(MainDestination.SETTINGS.route) {
                                SettingsScreen(
                                    settings = settings,
                                    hasPinConfigured = hasPinConfigured,
                                    biometricStatus = biometricStatus,
                                    authorizedBridgeStatus = authorizedBridgeStatus,
                                    onSetThemeMode = viewModel::setThemeMode,
                                    onSetFontScale = viewModel::setFontScale,
                                    onSetGridColumns = viewModel::setGridColumns,
                                    onSetSortOrder = viewModel::setSortOrder,
                                    onSetDefaultSection = viewModel::setDefaultSection,
                                    onSetSlideshowInterval = viewModel::setSlideshowInterval,
                                    onSetSlideshowLoop = viewModel::setSlideshowLoop,
                                    onConfigurePin = viewModel::configurePinAndEnableLock,
                                    onDisableAppLock = viewModel::disableAppLock,
                                    onSetBiometricEnabled = viewModel::setBiometricEnabled,
                                    onSetAutoLockTimeout = viewModel::setAutoLockTimeout,
                                    onLockNow = viewModel::lockNow,
                                    onClearThumbnailCache = viewModel::clearThumbnailCache
                                )
                            }

                            // GLOBAL SEARCH
                            composable(VaultRoutes.SEARCH) {
                                SearchScreen(
                                    query = searchQuery,
                                    results = searchResults,
                                    favoriteUris = favoriteUris,
                                    onQueryChange = viewModel::updateSearchQuery,
                                    onBack = { navController.popBackStack() },
                                    onOpenPhoto = { photo ->
                                        navController.navigate(VaultRoutes.photoViewer(photo.uri, "ALL"))
                                    },
                                    onOpenVideo = { video ->
                                        navController.navigate(VaultRoutes.videoPlayer(video.uri))
                                    },
                                    onOpenDocumentUri = openDocumentAction,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onShareUri = { uri, mime -> FormatUtils.shareUri(context, uri, mime) }
                                )
                            }

                            // FULL-SCREEN PHOTO VIEWER
                            composable(
                                route = VaultRoutes.PHOTO_VIEWER,
                                arguments = listOf(
                                    navArgument("encodedUri") { type = NavType.StringType },
                                    navArgument("bucketId") { type = NavType.StringType }
                                )
                            ) { backStack ->
                                val uri = VaultRoutes.decodeArg(backStack.arguments?.getString("encodedUri"))
                                val bucketId = VaultRoutes.decodeArg(backStack.arguments?.getString("bucketId"))
                                val albumPhotos by remember(bucketId) {
                                    viewModel.observeAlbumItems(bucketId)
                                }.collectAsStateWithLifecycle(initialValue = emptyList())

                                val viewerList = remember(bucketId, photos, albumPhotos) {
                                    if (bucketId == "ALL" || bucketId.isBlank()) photos
                                    else albumPhotos.filter { it.mediaType == MediaType.PHOTO }
                                }

                                PhotoViewerScreen(
                                    photos = viewerList,
                                    initialUri = uri,
                                    favoriteUris = favoriteUris,
                                    slideshowIntervalSec = settings.slideshowIntervalSec,
                                    slideshowLoop = settings.slideshowLoop,
                                    onBack = { navController.popBackStack() },
                                    onRecordRecent = { p ->
                                        viewModel.recordRecent(p.uri, MediaType.PHOTO, p.displayName, p.mimeType)
                                    },
                                    onToggleFavorite = { p ->
                                        viewModel.toggleFavorite(p.uri, MediaType.PHOTO, p.displayName, p.mimeType)
                                    },
                                    onDeleteMediaStoreConfirmed = viewModel::onMediaStoreItemDeleted,
                                    onDeleteSafItem = viewModel::deleteSafDocument,
                                    onRemoveMissing = viewModel::removeUnavailableReference,
                                    onShowMessage = viewModel::showMessage
                                )
                            }

                            // FULL-SCREEN VIDEO PLAYER
                            composable(
                                route = VaultRoutes.VIDEO_PLAYER,
                                arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
                            ) { backStack ->
                                val uri = VaultRoutes.decodeArg(backStack.arguments?.getString("encodedUri"))
                                val item = remember(uri, videos) { videos.firstOrNull { it.uri == uri } }
                                val name = item?.displayName ?: Uri.parse(uri).lastPathSegment ?: "Video"

                                VideoPlayerScreen(
                                    videoUri = uri,
                                    videoItem = item,
                                    isFavorite = uri in favoriteUris,
                                    onBack = { navController.popBackStack() },
                                    onRecordRecent = {
                                        viewModel.recordRecent(uri, MediaType.VIDEO, name, item?.mimeType ?: "video/*")
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(uri, MediaType.VIDEO, name, item?.mimeType ?: "video/*")
                                    },
                                    onRemoveMissing = { viewModel.removeUnavailableReference(uri) }
                                )
                            }

                            // FULL-SCREEN PDF READER
                            composable(
                                route = VaultRoutes.PDF_READER,
                                arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
                            ) { backStack ->
                                val uri = VaultRoutes.decodeArg(backStack.arguments?.getString("encodedUri"))
                                val docItem = remember(uri, documents) { documents.firstOrNull { it.uri == uri } }
                                val displayName = docItem?.displayName ?: Uri.parse(uri).lastPathSegment ?: "PDF Document"
                                val bookmarks by remember(uri) {
                                    viewModel.observeBookmarks(uri)
                                }.collectAsStateWithLifecycle(initialValue = emptyList())

                                var initialPage by remember(uri) { mutableIntStateOf(0) }
                                var loadedInitialPage by remember(uri) { mutableStateOf(false) }

                                LaunchedEffect(uri) {
                                    initialPage = viewModel.galleryRepository.getLastPdfPage(uri)
                                    loadedInitialPage = true
                                }

                                if (loadedInitialPage) {
                                    PdfReaderScreen(
                                        pdfUri = uri,
                                        displayName = displayName,
                                        initialPage = initialPage,
                                        isFavorite = uri in favoriteUris,
                                        bookmarks = bookmarks,
                                        onBack = { navController.popBackStack() },
                                        onPageChanged = { page ->
                                            viewModel.recordRecent(
                                                uri = uri,
                                                mediaType = MediaType.PDF,
                                                displayName = displayName,
                                                mimeType = "application/pdf",
                                                lastPage = page
                                            )
                                        },
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(uri, MediaType.PDF, displayName, "application/pdf")
                                        },
                                        onToggleBookmark = { page ->
                                            scope.launch {
                                                val added = viewModel.galleryRepository.togglePdfBookmark(uri, page)
                                                viewModel.showMessage(
                                                    if (added) "Bookmarked page ${page + 1}"
                                                    else "Removed bookmark on page ${page + 1}"
                                                )
                                            }
                                        },
                                        onDeleteBookmark = { id ->
                                            scope.launch { viewModel.galleryRepository.deleteBookmark(id) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isWideScreen) {
                contentScaffold()
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = showMainChrome,
                    drawerContent = {
                        ModalDrawerSheet(modifier = Modifier.width(292.dp)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Vault Gallery",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                            Text(
                                text = "Private Media & PDF Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            MainDestination.entries.forEach { dest ->
                                val selected = activeMainDestination == dest
                                NavigationDrawerItem(
                                    label = { Text(dest.label) },
                                    selected = selected,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigateToMainDestination(navController, dest)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_${dest.route}")
                                )
                            }
                        }
                    },
                    content = contentScaffold
                )
            }
        }

        // Full-Screen App Lock Security Gate
        if (isLocked && settings.appLockEnabled && hasPinConfigured) {
            val canBiometric = settings.biometricEnabled && biometricStatus == BiometricStatus.AVAILABLE
            AppLockOverlay(
                biometricAvailable = canBiometric,
                deviceCredentialAvailable = deviceCredentialAvailable,
                keystoreRecoveryNeeded = keystoreRecoveryNeeded,
                failedAttempts = failedAttempts,
                lockoutUntilMs = lockoutUntilMs,
                onVerifyPin = { pin -> viewModel.appLockManager.verifyAndUnlockWithPin(pin) },
                onRequestBiometric = {
                    viewModel.appLockManager.promptBiometricUnlock(
                        activity = activity,
                        onAuthenticated = {},
                        onError = { err -> viewModel.showMessage(err) }
                    )
                },
                onRequestDeviceCredentialRecovery = {
                    viewModel.appLockManager.promptDeviceCredentialRecovery(
                        activity = activity,
                        onRecovered = {
                            if (keystoreRecoveryNeeded) {
                                viewModel.resetAppLockAfterRecovery()
                            }
                        },
                        onUnavailableOrError = { err ->
                            if (keystoreRecoveryNeeded && !deviceCredentialAvailable) {
                                // Last-resort escape hatch if device has no OS lock screen & keystore reset
                                viewModel.resetAppLockAfterRecovery()
                            } else {
                                viewModel.showMessage(err)
                            }
                        }
                    )
                }
            )
        }
    }
}

private fun navigateToMainDestination(
    navController: NavHostController,
    destination: MainDestination
) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
