package com.example.vaultgallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.vaultgallery.data.db.VaultGalleryDatabase
import com.example.vaultgallery.data.media.MediaStoreScanner
import com.example.vaultgallery.data.repository.GalleryRepository
import com.example.vaultgallery.data.repository.SettingsRepository
import com.example.vaultgallery.security.AppLockManager
import com.example.vaultgallery.security.AuthorizedDataContract
import com.example.vaultgallery.security.KeystoreCryptoManager
import com.example.vaultgallery.security.SecureLocalStore

class VaultGalleryApplication : Application(), ImageLoaderFactory {

    lateinit var database: VaultGalleryDatabase
        private set

    lateinit var cryptoManager: KeystoreCryptoManager
        private set

    lateinit var secureStore: SecureLocalStore
        private set

    lateinit var appLockManager: AppLockManager
        private set

    lateinit var authorizedDataContract: AuthorizedDataContract
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var galleryRepository: GalleryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = VaultGalleryDatabase.create(this)
        cryptoManager = KeystoreCryptoManager()
        secureStore = SecureLocalStore(this, cryptoManager)
        appLockManager = AppLockManager(secureStore)
        authorizedDataContract = AuthorizedDataContract(secureStore)
        settingsRepository = SettingsRepository(database.appSettingsDao(), secureStore)
        galleryRepository = GalleryRepository(
            context = this,
            scanner = MediaStoreScanner(this),
            mediaItemDao = database.mediaItemDao(),
            recentItemDao = database.recentItemDao(),
            favoriteItemDao = database.favoriteItemDao(),
            bookmarkDao = database.bookmarkDao()
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("vault_thumb_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(180)
            .build()
    }
}
