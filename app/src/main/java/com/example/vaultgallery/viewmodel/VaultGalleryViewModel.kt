package com.example.vaultgallery.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.example.vaultgallery.VaultGalleryApplication
import com.example.vaultgallery.data.db.AlbumSummary
import com.example.vaultgallery.data.db.AppSettings
import com.example.vaultgallery.data.db.Bookmark
import com.example.vaultgallery.data.db.FavoriteItem
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.RecentItem
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.data.media.MediaPermissionManager
import com.example.vaultgallery.data.repository.GalleryRepository
import com.example.vaultgallery.data.repository.GroupedSearchResults
import com.example.vaultgallery.data.repository.SettingsRepository
import com.example.vaultgallery.security.AppLockManager
import com.example.vaultgallery.security.AuthorizedBridgeStatus
import com.example.vaultgallery.security.AuthorizedDataContract
import com.example.vaultgallery.security.SecureLocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class VaultGalleryViewModel(
    private val application: Application,
    val galleryRepository: GalleryRepository,
    private val settingsRepository: SettingsRepository,
    val appLockManager: AppLockManager,
    private val secureStore: SecureLocalStore,
    private val authorizedDataContract: AuthorizedDataContract
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _mediaAccessState = MutableStateFlow(
        MediaPermissionManager.checkMediaAccessState(application)
    )
    val mediaAccessState: StateFlow<MediaAccessState> = _mediaAccessState.asStateFlow()

    val mediaPermissionRequestedOnce: StateFlow<Boolean> = secureStore.mediaPermissionRequestedOnce
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<GroupedSearchResults> = _searchQuery
        .debounce(180L)
        .distinctUntilChanged()
        .flatMapLatest { query -> galleryRepository.searchGlobal(query) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupedSearchResults())

    private val sortOrderFlow: Flow<String> = settings
        .map { it.sortOrder }
        .distinctUntilChanged()

    private var activeSyncJob: Job? = null

    val photos: StateFlow<List<MediaItem>> = combine(
        galleryRepository.photos,
        sortOrderFlow
    ) { items, sortOrder -> sortMediaItems(items, sortOrder) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<MediaItem>> = combine(
        galleryRepository.videos,
        sortOrderFlow
    ) { items, sortOrder -> sortMediaItems(items, sortOrder) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentsAndPdfs: StateFlow<List<MediaItem>> = combine(
        galleryRepository.documentsAndPdfs,
        sortOrderFlow
    ) { items, sortOrder -> sortMediaItems(items, sortOrder) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumSummary>> = galleryRepository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteItem>> = galleryRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteUris: StateFlow<Set<String>> = galleryRepository.favoriteUris
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val recentItems: StateFlow<List<RecentItem>> = galleryRepository.recentItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<RecentItem>> = galleryRepository.recentDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLocked: StateFlow<Boolean> = appLockManager.isLocked
    val isLockStateResolved: StateFlow<Boolean> = appLockManager.isLockStateResolved
    val failedAttempts: StateFlow<Int> = appLockManager.failedAttempts
    val lockoutUntilMs: StateFlow<Long> = appLockManager.lockoutUntilMs
    val keystoreRecoveryNeeded: StateFlow<Boolean> = appLockManager.keystoreRecoveryNeeded
    val hasPinConfigured: StateFlow<Boolean> = appLockManager.hasPinConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val authorizedBridgeStatus: StateFlow<AuthorizedBridgeStatus> = authorizedDataContract.status
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AuthorizedBridgeStatus(isAuthorized = false, keystoreProtected = true, lastUpdatedMs = 0L)
        )

    init {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsOnce()
            appLockManager.evaluateInitialLock(currentSettings.appLockEnabled)
        }
        refreshPermissionsAndSync()
    }

    fun observeAlbumItems(bucketId: String): Flow<List<MediaItem>> {
        return combine(galleryRepository.observeAlbumItems(bucketId), sortOrderFlow) { items, sortOrder ->
            sortMediaItems(items, sortOrder)
        }.flowOn(Dispatchers.Default)
    }

    suspend fun getDefaultSectionOnce(): String = settingsRepository.getSettingsOnce().defaultSection

    fun observeBookmarks(pdfUri: String): Flow<List<Bookmark>> =
        galleryRepository.observeBookmarks(pdfUri)

    fun markMediaPermissionRequested() {
        viewModelScope.launch { secureStore.markMediaPermissionRequested() }
    }

    fun refreshPermissionsAndSync(force: Boolean = false) {
        val access = MediaPermissionManager.checkMediaAccessState(application)
        _mediaAccessState.value = access
        if (!force && activeSyncJob?.isActive == true) return
        activeSyncJob = viewModelScope.launch {
            _isSyncing.value = true
            try {
                runCatching { galleryRepository.syncDeviceMedia() }
                    .onFailure { showMessage("Unable to scan device media right now.") }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun onAppBackgrounded() {
        viewModelScope.launch { appLockManager.onAppBackgrounded() }
    }

    fun onAppForegrounded() {
        refreshPermissionsAndSync()
        viewModelScope.launch {
            val s = settingsRepository.getSettingsOnce()
            appLockManager.onAppForegrounded(s.appLockEnabled, s.autoLockTimeoutSec)
        }
    }

    fun importPickedPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = galleryRepository.importPickedMedia(uris, "Picked Photos")
            if (imported.isNotEmpty()) {
                showMessage("Added ${imported.size} item${if (imported.size == 1) "" else "s"} to gallery")
            }
        }
    }

    fun importPickedVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = galleryRepository.importPickedMedia(uris, "Picked Videos")
            if (imported.isNotEmpty()) {
                showMessage("Added ${imported.size} video${if (imported.size == 1) "" else "s"}")
            }
        }
    }

    fun importSafDocuments(uris: List<Uri>, onFirstPdfImported: ((String) -> Unit)? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = galleryRepository.importSafDocuments(uris)
            if (imported.isNotEmpty()) {
                showMessage("Linked ${imported.size} document${if (imported.size == 1) "" else "s"}")
                if (imported.size == 1 && imported.first().mediaType == com.example.vaultgallery.data.db.MediaType.PDF) {
                    onFirstPdfImported?.invoke(imported.first().uri)
                }
            } else {
                showMessage("Could not read selected document.")
            }
        }
    }

    fun importSafFolder(treeUri: Uri?) {
        if (treeUri == null) return
        viewModelScope.launch {
            _isSyncing.value = true
            val count = galleryRepository.importSafFolder(treeUri)
            _isSyncing.value = false
            if (count > 0) {
                showMessage("Linked folder with $count items")
            } else {
                showMessage("No readable media or documents found in folder")
            }
        }
    }

    fun toggleFavorite(uri: String, mediaType: String, displayName: String, mimeType: String = "") {
        viewModelScope.launch {
            val nowFav = galleryRepository.toggleFavorite(uri, mediaType, displayName, mimeType)
            showMessage(if (nowFav) "Added to Favorites" else "Removed from Favorites")
        }
    }

    fun recordRecent(
        uri: String,
        mediaType: String,
        displayName: String,
        mimeType: String = "",
        lastPage: Int? = null
    ) {
        viewModelScope.launch {
            galleryRepository.recordRecentAccess(uri, mediaType, displayName, mimeType, lastPage)
        }
    }

    fun renameDocument(item: MediaItem, newName: String) {
        viewModelScope.launch {
            val renamed = galleryRepository.renameDocument(item, newName)
            showMessage(
                if (renamed) "Renamed to $newName"
                else "Rename is not permitted by this storage provider."
            )
        }
    }

    fun deleteSafDocument(item: MediaItem) {
        viewModelScope.launch {
            val deleted = galleryRepository.deleteSafItem(item.uri)
            showMessage(
                if (deleted) "Deleted ${item.displayName}"
                else "Removed ${item.displayName} from Vault Gallery"
            )
        }
    }

    fun onMediaStoreItemDeleted(uri: String) {
        viewModelScope.launch {
            galleryRepository.removeLocalRecords(uri)
            showMessage("Item deleted")
        }
    }

    fun removeUnavailableReference(uri: String) {
        viewModelScope.launch {
            galleryRepository.removeLocalRecords(uri)
            showMessage("Removed missing item from library")
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearThumbnailCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    application.imageLoader.memoryCache?.clear()
                    application.imageLoader.diskCache?.clear()
                }
            }
            showMessage("Thumbnail cache cleared")
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    // Settings mutations
    fun setThemeMode(mode: String) = updateSetting { it.copy(themeMode = mode) }
    fun setFontScale(scale: Float) = updateSetting { it.copy(fontScale = scale) }
    fun setGridColumns(columns: Int) = updateSetting { it.copy(gridColumns = columns.coerceIn(2, 5)) }
    fun setSortOrder(order: String) = updateSetting { it.copy(sortOrder = order) }
    fun setDefaultSection(section: String) = updateSetting { it.copy(defaultSection = section) }
    fun setSlideshowInterval(seconds: Int) = updateSetting { it.copy(slideshowIntervalSec = seconds.coerceIn(2, 15)) }
    fun setSlideshowLoop(loop: Boolean) = updateSetting { it.copy(slideshowLoop = loop) }
    fun setBiometricEnabled(enabled: Boolean) = updateSetting { it.copy(biometricEnabled = enabled) }
    fun setAutoLockTimeout(seconds: Int) = updateSetting { it.copy(autoLockTimeoutSec = seconds) }

    fun configurePinAndEnableLock(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val saved = appLockManager.setPin(pin)
            if (saved) {
                settingsRepository.updateSettings { it.copy(appLockEnabled = true) }
                showMessage("App Lock enabled with Keystore encryption")
            }
            onResult(saved)
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(appLockEnabled = false) }
            appLockManager.unlockDirectly()
            showMessage("App Lock disabled")
        }
    }

    fun resetAppLockAfterRecovery() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(appLockEnabled = false) }
            appLockManager.clearPinAndUnlock()
            showMessage("App Lock reset after device verification")
        }
    }

    fun lockNow() {
        viewModelScope.launch {
            if (appLockManager.hasPinConfigured.first()) {
                settingsRepository.updateSettings { it.copy(appLockEnabled = true) }
                appLockManager.lockNow()
            } else {
                showMessage("Set a Vault PIN first to lock the app")
            }
        }
    }

    private fun updateSetting(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    private fun sortMediaItems(items: List<MediaItem>, sortOrder: String): List<MediaItem> {
        return when (sortOrder) {
            "DATE_ASC" -> items.sortedBy { it.dateModified }
            "NAME_ASC" -> items.sortedBy { it.displayName.lowercase() }
            "NAME_DESC" -> items.sortedByDescending { it.displayName.lowercase() }
            "SIZE_DESC" -> items.sortedByDescending { it.sizeBytes }
            else -> items.sortedByDescending { it.dateModified }
        }
    }

    companion object {
        fun factory(application: VaultGalleryApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VaultGalleryViewModel(
                        application = application,
                        galleryRepository = application.galleryRepository,
                        settingsRepository = application.settingsRepository,
                        appLockManager = application.appLockManager,
                        secureStore = application.secureStore,
                        authorizedDataContract = application.authorizedDataContract
                    ) as T
                }
            }
        }
    }
}
