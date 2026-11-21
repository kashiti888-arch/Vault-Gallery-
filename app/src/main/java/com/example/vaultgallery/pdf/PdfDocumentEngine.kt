package com.example.vaultgallery.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException

sealed interface PdfOpenResult {
    data class Success(val pageCount: Int, val fileSizeBytes: Long) : PdfOpenResult
    data object PasswordProtected : PdfOpenResult
    data class CorruptedOrUnsupported(val reason: String) : PdfOpenResult
    data class Inaccessible(val reason: String) : PdfOpenResult
}

data class PdfSearchMatch(
    val pageIndex: Int,
    val snippet: String
)

/**
 * Memory-safe PDF renderer powered by Android's native PdfRenderer.
 * - Uses ARGB_8888 strictly (required by Android PdfRenderer.Page.render).
 * - Preserves exact aspect ratio across portrait, landscape, scanned, and rotated PDFs.
 * - Serializes native PdfRenderer calls via a coroutine Mutex and bounds bitmap RAM via LruCache.
 * - Never crashes on 1-page, multi-page, large, corrupted, empty, inaccessible, or password-protected PDFs.
 */
class PdfDocumentEngine(private val context: Context) : Closeable {

    private val renderMutex = Mutex()
    @Volatile
    private var isClosed: Boolean = false
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var activeUri: Uri? = null

    private val pageCache = object : LruCache<String, Bitmap>(16 * 1024) { // 16 MB max
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    private val thumbCache = object : LruCache<Int, Bitmap>(6 * 1024) { // 6 MB max
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    val pageCount: Int
        get() = renderer?.pageCount ?: 0

    suspend fun open(uri: Uri): PdfOpenResult = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            isClosed = false
            closeInternal()
            activeUri = uri
            val pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (sec: SecurityException) {
                return@withLock PdfOpenResult.Inaccessible(
                    "Storage access to this PDF was revoked or denied."
                )
            } catch (e: Exception) {
                return@withLock PdfOpenResult.Inaccessible(
                    "Document could not be found or opened."
                )
            } ?: return@withLock PdfOpenResult.Inaccessible("File descriptor is unavailable.")

            val fileSize = runCatching { pfd.statSize }.getOrDefault(-1L)
            if (fileSize == 0L) {
                runCatching { pfd.close() }
                return@withLock PdfOpenResult.CorruptedOrUnsupported("The selected PDF file is empty.")
            }

            // Fast pre-flight check for valid PDF magic header (%PDF-) before native PdfRenderer
            val hasPdfHeader = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val header = ByteArray(1024)
                    val bytesRead = stream.read(header)
                    bytesRead > 4 && String(header, 0, bytesRead, Charsets.ISO_8859_1).contains("%PDF-")
                } ?: false
            }.getOrDefault(false)

            if (!hasPdfHeader) {
                runCatching { pfd.close() }
                return@withLock PdfOpenResult.CorruptedOrUnsupported(
                    "This file is not a valid PDF document or its header is corrupted."
                )
            }

            return@withLock try {
                val pdfRenderer = PdfRenderer(pfd)
                if (pdfRenderer.pageCount <= 0) {
                    pdfRenderer.close()
                    pfd.close()
                    PdfOpenResult.CorruptedOrUnsupported("This PDF contains no renderable pages.")
                } else {
                    fileDescriptor = pfd
                    renderer = pdfRenderer
                    PdfOpenResult.Success(pdfRenderer.pageCount, fileSize.coerceAtLeast(0L))
                }
            } catch (sec: SecurityException) {
                runCatching { pfd.close() }
                PdfOpenResult.PasswordProtected
            } catch (io: IOException) {
                runCatching { pfd.close() }
                PdfOpenResult.CorruptedOrUnsupported("This PDF file is corrupted or malformed.")
            } catch (t: Throwable) {
                runCatching { pfd.close() }
                PdfOpenResult.CorruptedOrUnsupported(t.localizedMessage ?: "Unsupported PDF format.")
            }
        }
    }

    suspend fun renderPage(
        pageIndex: Int,
        targetWidthPx: Int,
        rotationDegrees: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val clampedWidth = targetWidthPx.coerceIn(360, 2048)
        val cacheKey = "${pageIndex}_${clampedWidth}_$normalizedRotation"
        pageCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return@withContext it }

        renderMutex.withLock {
            if (isClosed) {
                closeInternal()
                return@withLock null
            }
            val activeRenderer = renderer ?: return@withLock null
            if (pageIndex !in 0 until activeRenderer.pageCount) return@withLock null

            try {
                runCatching {
                    activeRenderer.openPage(pageIndex).use { page ->
                        val srcW = page.width.coerceAtLeast(1).toFloat()
                        val srcH = page.height.coerceAtLeast(1).toFloat()
                        val swapAxes = normalizedRotation == 90 || normalizedRotation == 270
                        val effectiveW = if (swapAxes) srcH else srcW
                        val maxDim = 2200f
                        val desiredScale = (clampedWidth.toFloat() / effectiveW).coerceIn(0.4f, 3.0f)
                        val maxSafeScale = maxDim / maxOf(srcW, srcH)
                        val finalScale = minOf(desiredScale, maxSafeScale).coerceAtLeast(0.25f)

                        val bmpW = (srcW * finalScale).toInt().coerceAtLeast(64)
                        val bmpH = (srcH * finalScale).toInt().coerceAtLeast(64)

                        val baseBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                        baseBitmap.eraseColor(Color.WHITE)
                        page.render(baseBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val finalBitmap = if (normalizedRotation != 0) {
                            val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
                            val rotated = Bitmap.createBitmap(
                                baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true
                            )
                            if (rotated !== baseBitmap) {
                                baseBitmap.recycle()
                            }
                            rotated
                        } else {
                            baseBitmap
                        }

                        if (!isClosed) {
                            pageCache.put(cacheKey, finalBitmap)
                        }
                        finalBitmap
                    }
                }.getOrNull()
            } finally {
                if (isClosed) closeInternal()
            }
        }
    }

    suspend fun renderThumbnail(pageIndex: Int, thumbWidthPx: Int = 180): Bitmap? =
        withContext(Dispatchers.IO) {
            thumbCache.get(pageIndex)?.takeIf { !it.isRecycled }?.let { return@withContext it }

            renderMutex.withLock {
                if (isClosed) {
                    closeInternal()
                    return@withLock null
                }
                val activeRenderer = renderer ?: return@withLock null
                if (pageIndex !in 0 until activeRenderer.pageCount) return@withLock null

                try {
                    runCatching {
                        activeRenderer.openPage(pageIndex).use { page ->
                            val srcW = page.width.coerceAtLeast(1).toFloat()
                            val srcH = page.height.coerceAtLeast(1).toFloat()
                            val maxSide = thumbWidthPx.coerceIn(96, 260).toFloat()
                            val scale = minOf(maxSide / srcW, (maxSide * 1.6f) / srcH).coerceAtLeast(0.1f)
                            val w = (srcW * scale).toInt().coerceAtLeast(48)
                            val h = (srcH * scale).toInt().coerceAtLeast(48)
                            // ARGB_8888 is strictly required by Android PdfRenderer.Page.render()
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            if (!isClosed) {
                                thumbCache.put(pageIndex, bitmap)
                            }
                            bitmap
                        }
                    }.getOrNull()
                } finally {
                    if (isClosed) closeInternal()
                }
            }
        }

    /**
     * Bounded-memory PDF search supporting direct page jump and text stream lookup.
     */
    suspend fun searchInPdf(query: String): List<PdfSearchMatch> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val totalPages = pageCount
        if (totalPages <= 0) return@withContext emptyList()

        val results = mutableListOf<PdfSearchMatch>()

        val pageNum = trimmed.removePrefix("page").removePrefix("p.").trim().toIntOrNull()
        if (pageNum != null && pageNum in 1..totalPages) {
            results.add(PdfSearchMatch(pageIndex = pageNum - 1, snippet = "Jump directly to page $pageNum"))
        }

        val uri = activeUri ?: return@withContext results
        runCatching {
            val maxScanBytes = 2 * 1024 * 1024
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val out = ByteArrayOutputStream()
                val chunk = ByteArray(32 * 1024)
                var totalRead = 0
                while (totalRead < maxScanBytes) {
                    val toRead = minOf(chunk.size, maxScanBytes - totalRead)
                    val n = stream.read(chunk, 0, toRead)
                    if (n <= 0) break
                    out.write(chunk, 0, n)
                    totalRead += n
                }
                val bytes = out.toByteArray()
                if (bytes.isNotEmpty()) {
                    val raw = String(bytes, Charsets.ISO_8859_1)
                    val lowerRaw = raw.lowercase()
                    val lowerQuery = trimmed.lowercase()
                    var idx = lowerRaw.indexOf(lowerQuery)
                    var matchCount = 0
                    while (idx >= 0 && matchCount < 20) {
                        val approxPage = ((idx.toFloat() / raw.length.coerceAtLeast(1)) * totalPages)
                            .toInt()
                            .coerceIn(0, totalPages - 1)
                        val start = (idx - 24).coerceAtLeast(0)
                        val end = (idx + trimmed.length + 32).coerceAtMost(raw.length)
                        val cleaned = raw.substring(start, end)
                            .replace(Regex("[^\\x20-\\x7E]"), " ")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                        if (cleaned.isNotBlank() && results.none { it.pageIndex == approxPage }) {
                            results.add(PdfSearchMatch(pageIndex = approxPage, snippet = "…$cleaned…"))
                            matchCount++
                        }
                        idx = lowerRaw.indexOf(lowerQuery, idx + lowerQuery.length)
                    }
                }
            }
        }
        results
    }

    private fun closeInternal() {
        pageCache.evictAll()
        thumbCache.evictAll()
        runCatching { renderer?.close() }
        runCatching { fileDescriptor?.close() }
        renderer = null
        fileDescriptor = null
        activeUri = null
    }

    suspend fun closeSafely() = withContext(Dispatchers.IO) {
        isClosed = true
        renderMutex.withLock {
            closeInternal()
        }
    }

    override fun close() {
        isClosed = true
        if (renderMutex.tryLock()) {
            try {
                closeInternal()
            } finally {
                renderMutex.unlock()
            }
        }
    }
}
