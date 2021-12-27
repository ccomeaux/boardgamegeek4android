package com.boardgamegeek.util

import android.content.Context
import com.boardgamegeek.provider.BggContract
import java.io.File
import java.io.IOException

object FileUtils {
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

    /**
     * Recursively delete everything in `dir`.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun deleteContents(directory: File?): Int {
        // TODO: this should specify paths as Strings rather than as Files
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
