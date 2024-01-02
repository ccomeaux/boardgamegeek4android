package com.boardgamegeek.util

import android.content.Context
import android.graphics.Bitmap
import com.boardgamegeek.provider.BggContract
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {
    fun getFile(context: Context, type: String?, url: String?): File? {
        if (url.isNullOrBlank()) return null
        val filename = getFileNameFromUrl(url)
        return if (filename.isNotBlank())
            File(generateContentPath(context, type), filename)
        else null
    }

    /**
     * Returns a usable filename from the specified URL.
     */
    fun getFileNameFromUrl(url: String?): String {
        return if (!url.isNullOrBlank() && BggContract.INVALID_URL != url) {
            url.substringAfterLast('/', "")
        } else ""
    }

    /**
     * Find a path to store the specific type of content, ensuring that it exists. Returns null if none can be found or
     * created.
     */
    fun generateContentPath(context: Context, type: String?): File? {
        val base = context.getExternalFilesDir(type) ?: return null
        if (!base.exists()) {
            if (!base.mkdirs()) {
                return null
            }
        }
        return base
    }

    fun saveBitmap(file: File, bitmap: Bitmap) {
        try {
            BufferedOutputStream(FileOutputStream(file)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        } catch (e: IOException) {
            Timber.e(e, "Error saving the thumbnail file at ${file.name}.")
        }
    }

    /**
     * Recursively delete everything in `dir`.
     */
    fun deleteContents(directory: File?): Int {
        if (directory == null || !directory.exists()) {
            return 0
        }
        val files = directory.listFiles() ?: throw IllegalArgumentException("not a directory: $directory")
        var count = 0
        for (file in files) {
            if (file.isDirectory) {
                count += deleteContents(file)
            }
            if (!file.delete()) {
                throw IOException("failed to delete file: $file")
            }
            count++
        }
        return count
    }

    fun getExportFileName(type: String) = "bgg4a-$type.json"
}
