package com.geno1024.pictureframe.io

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ExportFormat(val label: String, val mime: String, val extension: String, val compress: Bitmap.CompressFormat) {
    Jpeg("JPG", "image/jpeg", "jpg", Bitmap.CompressFormat.JPEG),
    Png("PNG", "image/png", "png", Bitmap.CompressFormat.PNG),
}

private const val ALBUM = "PictureFrame"

suspend fun saveToGallery(
    context: Context,
    bitmap: Bitmap,
    format: ExportFormat,
    displayName: String,
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveViaMediaStore(context, bitmap, format, displayName)
        else saveViaLegacyFile(context, bitmap, format, displayName)
    }.getOrNull()
}

private fun saveViaMediaStore(
    context: Context,
    bitmap: Bitmap,
    format: ExportFormat,
    displayName: String,
): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, format.mime)
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return null
    try {
        resolver.openOutputStream(uri)?.use { write(bitmap, format, it) } ?: error("no stream")
    } catch (error: Throwable) {
        resolver.delete(uri, null, null)
        throw error
    }
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    return uri
}

private fun saveViaLegacyFile(
    context: Context,
    bitmap: Bitmap,
    format: ExportFormat,
    displayName: String,
): Uri? {
    @Suppress("DEPRECATION")
    val album = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
    if (!album.exists() && !album.mkdirs()) return null
    val file = File(album, displayName)
    FileOutputStream(file).use { write(bitmap, format, it) }
    var scanned: Uri? = null
    MediaScannerConnection.scanFile(
        context,
        arrayOf(file.absolutePath),
        arrayOf(format.mime),
    ) { _, uri -> scanned = uri }
    return scanned ?: Uri.fromFile(file)
}

private fun write(bitmap: Bitmap, format: ExportFormat, out: OutputStream) {
    if (!bitmap.compress(format.compress, 96, out)) error("compress failed")
}
