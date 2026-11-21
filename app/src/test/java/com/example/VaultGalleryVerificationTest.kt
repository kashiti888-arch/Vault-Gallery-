package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.vaultgallery.data.db.AppSettings
import com.example.vaultgallery.data.db.Bookmark
import com.example.vaultgallery.data.db.FavoriteItem
import com.example.vaultgallery.data.db.MediaItem
import com.example.vaultgallery.data.db.MediaSource
import com.example.vaultgallery.data.db.MediaType
import com.example.vaultgallery.data.db.RecentItem
import com.example.vaultgallery.data.db.VaultGalleryDatabase
import com.example.vaultgallery.data.media.MediaAccessState
import com.example.vaultgallery.data.media.MediaPermissionManager
import com.example.vaultgallery.data.media.MediaStoreScanner
import com.example.vaultgallery.data.repository.GalleryRepository
import com.example.vaultgallery.pdf.PdfDocumentEngine
import com.example.vaultgallery.pdf.PdfOpenResult
import com.example.vaultgallery.security.AppLockManager
import com.example.vaultgallery.security.KeystoreCryptoManager
import com.example.vaultgallery.security.PinVerificationResult
import com.example.vaultgallery.security.SecureLocalStore
import com.example.vaultgallery.ui.navigation.VaultRoutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultGalleryVerificationTest {

    private lateinit var context: Context
    private lateinit var db: VaultGalleryDatabase
    private lateinit var repository: GalleryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VaultGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GalleryRepository(
            context = context,
            scanner = MediaStoreScanner(context),
            mediaItemDao = db.mediaItemDao(),
            recentItemDao = db.recentItemDao(),
            favoriteItemDao = db.favoriteItemDao(),
            bookmarkDao = db.bookmarkDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `large gallery over 1500 photos syncs atomically and prunes deleted items while preserving SAF items`() = runTest {
        val safDoc = MediaItem(
            uri = "content://com.android.externalstorage.documents/document/primary%3Akeep.jpg",
            displayName = "keep.jpg",
            mediaType = MediaType.PHOTO,
            mimeType = "image/jpeg",
            dateModified = 50_000L,
            bucketId = "saf_folder",
            bucketName = "Linked",
            source = MediaSource.SAF,
            lastSyncedAt = 0L
        )
        db.mediaItemDao().upsert(safDoc)

        val sync1 = 1_000L
        val batch1 = (1..1550).map { i ->
            MediaItem(
                uri = "content://media/external/images/media/$i",
                displayName = "IMG_$i.jpg",
                mediaType = MediaType.PHOTO,
                mimeType = "image/jpeg",
                dateModified = 10_000L + i,
                bucketId = "camera",
                bucketName = "Camera",
                source = MediaSource.MEDIASTORE,
                lastSyncedAt = sync1
            )
        }
        db.mediaItemDao().replaceMediaStoreBatch(MediaSource.MEDIASTORE, MediaType.PHOTO, batch1, sync1)
        assertEquals(1551, repository.photos.first().size)

        // Simulate 150 photos deleted on device during sync2
        val sync2 = 2_000L
        val surviving = batch1.take(1400).map { it.copy(lastSyncedAt = sync2) }
        db.mediaItemDao().replaceMediaStoreBatch(MediaSource.MEDIASTORE, MediaType.PHOTO, surviving, sync2)

        val afterPrune = repository.photos.first()
        assertEquals(1401, afterPrune.size)
        assertTrue(afterPrune.any { it.uri == safDoc.uri })
    }

    @Test
    fun `albums group photos and videos with accurate counts and latest cover`() = runTest {
        db.mediaItemDao().upsertAll(
            listOf(
                MediaItem(
                    uri = "content://media/1",
                    displayName = "old.jpg",
                    mediaType = MediaType.PHOTO,
                    mimeType = "image/jpeg",
                    dateModified = 100L,
                    bucketId = "b_trip",
                    bucketName = "Kyoto Trip"
                ),
                MediaItem(
                    uri = "content://media/2",
                    displayName = "newest.mp4",
                    mediaType = MediaType.VIDEO,
                    mimeType = "video/mp4",
                    dateModified = 500L,
                    durationMs = 12_000L,
                    bucketId = "b_trip",
                    bucketName = "Kyoto Trip"
                ),
                MediaItem(
                    uri = "content://media/3",
                    displayName = "doc.pdf",
                    mediaType = MediaType.PDF,
                    mimeType = "application/pdf",
                    dateModified = 900L,
                    bucketId = "b_docs",
                    bucketName = "Documents"
                )
            )
        )
        val albums = repository.albums.first()
        assertEquals(1, albums.size)
        assertEquals("Kyoto Trip", albums.first().bucketName)
        assertEquals(2, albums.first().itemCount)
        assertEquals("content://media/2", albums.first().coverUri)
    }

    @Test
    fun `case-insensitive global search groups photos videos pdfs documents favorites and recents`() = runTest {
        db.mediaItemDao().upsertAll(
            listOf(
                MediaItem(uri = "u://1", displayName = "Sunset_Invoice.jpg", mediaType = MediaType.PHOTO, mimeType = "image/jpeg"),
                MediaItem(uri = "u://2", displayName = "INVOICE_walkthrough.mp4", mediaType = MediaType.VIDEO, mimeType = "video/mp4"),
                MediaItem(uri = "u://3", displayName = "Tax_Invoice_2026.pdf", mediaType = MediaType.PDF, mimeType = "application/pdf"),
                MediaItem(uri = "u://4", displayName = "invoice_notes.docx", mediaType = MediaType.DOCUMENT, mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            )
        )
        db.favoriteItemDao().upsert(FavoriteItem(uri = "u://3", mediaType = MediaType.PDF, displayName = "Tax_Invoice_2026.pdf"))
        db.recentItemDao().upsert(RecentItem(uri = "u://1", mediaType = MediaType.PHOTO, displayName = "Sunset_Invoice.jpg"))

        val grouped = repository.searchGlobal("invoice").first()
        assertEquals(1, grouped.photos.size)
        assertEquals(1, grouped.videos.size)
        assertEquals(1, grouped.pdfs.size)
        assertEquals(1, grouped.documents.size)
        assertEquals(1, grouped.favorites.size)
        assertEquals(1, grouped.recents.size)
        assertEquals(6, grouped.totalCount)

        val empty = repository.searchGlobal("nonexistent_xyz").first()
        assertEquals(0, empty.totalCount)
    }

    @Test
    fun `search escapes SQL LIKE wildcards underscore and percent accurately`() = runTest {
        db.mediaItemDao().upsertAll(
            listOf(
                MediaItem(uri = "u://u1", displayName = "IMG_2026.jpg", mediaType = MediaType.PHOTO, mimeType = "image/jpeg"),
                MediaItem(uri = "u://u2", displayName = "IMGX2026.jpg", mediaType = MediaType.PHOTO, mimeType = "image/jpeg"),
                MediaItem(uri = "u://p1", displayName = "Budget_50%.pdf", mediaType = MediaType.PDF, mimeType = "application/pdf"),
                MediaItem(uri = "u://p2", displayName = "Budget_500.pdf", mediaType = MediaType.PDF, mimeType = "application/pdf")
            )
        )

        val underscoreMatch = repository.searchGlobal("IMG_2026").first()
        assertEquals(1, underscoreMatch.photos.size)
        assertEquals("u://u1", underscoreMatch.photos.first().uri)

        val percentMatch = repository.searchGlobal("50%").first()
        assertEquals(1, percentMatch.pdfs.size)
        assertEquals("u://p1", percentMatch.pdfs.first().uri)
    }

    @Test
    fun `favorites bookmarks and settings persist cleanly and deleted files prune records`() = runTest {
        val added = repository.toggleFavorite("u://doc1", MediaType.PDF, "Contract.pdf", "application/pdf")
        assertTrue(added)
        assertTrue(repository.observeIsFavorite("u://doc1").first())

        val bookmarked = repository.togglePdfBookmark("u://doc1", pageIndex = 4, customLabel = "Signature Page")
        assertTrue(bookmarked)
        val bookmarks = repository.observeBookmarks("u://doc1").first()
        assertEquals(1, bookmarks.size)
        assertEquals(4, bookmarks.first().pageIndex)

        db.appSettingsDao().upsert(AppSettings(themeMode = "LIGHT", gridColumns = 4, autoLockTimeoutSec = 300))
        val savedSettings = db.appSettingsDao().getSettings()!!
        assertEquals("LIGHT", savedSettings.themeMode)
        assertEquals(4, savedSettings.gridColumns)
        assertEquals(300, savedSettings.autoLockTimeoutSec)

        repository.removeLocalRecords("u://doc1")
        assertFalse(repository.observeIsFavorite("u://doc1").first())
        assertTrue(repository.observeBookmarks("u://doc1").first().isEmpty())
    }

    @Test
    fun `bookmark URI updates cleanly when a PDF document is renamed`() = runTest {
        val oldUri = "content://docs/old.pdf"
        val newUri = "content://docs/renamed.pdf"
        repository.togglePdfBookmark(oldUri, pageIndex = 2, customLabel = "Summary")
        assertEquals(1, repository.observeBookmarks(oldUri).first().size)

        db.bookmarkDao().updatePdfUri(oldUri, newUri)
        assertTrue(repository.observeBookmarks(oldUri).first().isEmpty())
        val moved = repository.observeBookmarks(newUri).first()
        assertEquals(1, moved.size)
        assertEquals(2, moved.first().pageIndex)
    }

    @Test
    fun `VaultRoutes preserves SAF document URIs containing percent-encoded colons and slashes`() {
        val safUri = "content://com.android.externalstorage.documents/document/primary%3ADocuments%2FMy%20Tax%20Report.pdf"
        val encoded = VaultRoutes.encodeArg(safUri)
        val decoded = VaultRoutes.decodeArg(encoded)
        assertEquals(safUri, decoded)
    }

    @Test
    fun `PdfDocumentEngine gracefully handles empty corrupted missing files and safe close`() = runTest {
        val engine = PdfDocumentEngine(context)

        val missingRes = engine.open(Uri.fromFile(File(context.cacheDir, "does_not_exist_12345.pdf")))
        assertTrue(missingRes is PdfOpenResult.Inaccessible)

        val emptyFile = File(context.cacheDir, "empty_test.pdf").apply { writeBytes(ByteArray(0)) }
        val emptyRes = engine.open(Uri.fromFile(emptyFile))
        assertTrue(emptyRes is PdfOpenResult.CorruptedOrUnsupported)
        emptyFile.delete()

        val corruptFile = File(context.cacheDir, "corrupt_test.pdf").apply {
            writeText("NOT A VALID PDF HEADER %% garbage bytes 12345")
        }
        val corruptRes = engine.open(Uri.fromFile(corruptFile))
        assertTrue(corruptRes is PdfOpenResult.CorruptedOrUnsupported || corruptRes is PdfOpenResult.Inaccessible)
        corruptFile.delete()

        engine.closeSafely()
    }

    @Test
    fun `AppLockManager verifies hashed PIN clears background timestamp on unlock enforces lockout and recovers`() = runTest {
        val store = SecureLocalStore(context, KeystoreCryptoManager("test_key_vault_gallery"))
        val lockManager = AppLockManager(store)

        assertTrue(lockManager.setPin("2580"))
        lockManager.evaluateInitialLock(appLockEnabled = true)
        assertTrue(lockManager.isLocked.value)

        assertEquals(PinVerificationResult.INVALID, lockManager.verifyAndUnlockWithPin("1111"))
        assertTrue(lockManager.isLocked.value)

        // Background + immediate auto-lock re-locks on foreground
        lockManager.onAppBackgrounded()
        assertEquals(PinVerificationResult.VALID, lockManager.verifyAndUnlockWithPin("2580"))
        assertFalse(lockManager.isLocked.value)
        // Unlocking clears stale background timestamp so foreground check does not spuriously re-lock
        lockManager.onAppForegrounded(appLockEnabled = true, autoLockTimeoutSec = 0)
        assertFalse(lockManager.isLocked.value)

        // Genuine background followed by foreground re-locks
        lockManager.onAppBackgrounded()
        lockManager.onAppForegrounded(appLockEnabled = true, autoLockTimeoutSec = 0)
        assertTrue(lockManager.isLocked.value)

        // 5 consecutive wrong PINs trigger temporary lockout (not permanent)
        repeat(5) {
            lockManager.verifyAndUnlockWithPin("0000")
        }
        assertTrue(lockManager.lockoutUntilMs.value > System.currentTimeMillis())

        // Recovery unlock clears lock and lockout
        lockManager.clearPinAndUnlock()
        assertFalse(lockManager.isLocked.value)
        assertEquals(0L, lockManager.lockoutUntilMs.value)
    }

    @Test
    fun `denied media permission clears MediaStore cache while preserving SAF and Picker items`() = runTest {
        val state = MediaPermissionManager.checkMediaAccessState(context)
        assertEquals(MediaAccessState.DENIED, state)
        assertFalse(MediaPermissionManager.canReadImages(context))
        assertFalse(MediaPermissionManager.canReadVideos(context))
        assertTrue(MediaPermissionManager.requiredMediaPermissions().isNotEmpty())

        db.mediaItemDao().upsertAll(
            listOf(
                MediaItem(uri = "ms://1", displayName = "cam.jpg", mediaType = MediaType.PHOTO, mimeType = "image/jpeg", source = MediaSource.MEDIASTORE),
                MediaItem(uri = "picker://1", displayName = "picked.jpg", mediaType = MediaType.PHOTO, mimeType = "image/jpeg", source = MediaSource.PICKER)
            )
        )
        val synced = repository.syncDeviceMedia()
        assertEquals(0, synced)
        val remaining = repository.photos.first()
        assertEquals(1, remaining.size)
        assertEquals("picker://1", remaining.first().uri)
    }
}
