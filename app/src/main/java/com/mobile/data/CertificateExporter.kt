package com.mobile.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Saves rendered certificate Bitmaps to internal storage (so "previously generated
 * certificates" survive app restarts and can be re-downloaded/re-shared later) and builds the
 * share/download plumbing around them. Internal-storage files can't be handed to another app
 * as a raw file:// Uri (FileUriExposedException on API 24+), so sharing goes through
 * FileProvider — see the matching <provider> entry in AndroidManifest.xml and res/xml/file_paths.xml.
 */
object CertificateExporter {
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    fun certificatesDir(context: Context): File =
        File(context.filesDir, "certificates").apply { mkdirs() }

    fun photosDir(context: Context): File =
        File(context.filesDir, "certificate_photos").apply { mkdirs() }

    fun savePng(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(certificatesDir(context), fileName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }

    /** Renders [bitmap] as a single full-page PDF. */
    fun savePdf(bitmap: Bitmap, destination: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)
        FileOutputStream(destination).use { out -> document.writeTo(out) }
        document.close()
    }

    /** Copies a user-picked photo into internal storage so the certificate keeps working even if the original photo is later deleted from the gallery. */
    fun savePhoto(context: Context, uri: Uri, fileName: String): String? = try {
        val file = File(photosDir(context), fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)

    /** Streams [source]'s bytes into an already-opened SAF destination (from a CreateDocument picker). */
    fun copyToUri(context: Context, source: File, destination: Uri) {
        context.contentResolver.openOutputStream(destination)?.use { out ->
            source.inputStream().use { input -> input.copyTo(out) }
        }
    }

    fun shareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = uriFor(context, file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share your certificate")
    }
}
