package com.boardgamegeek.util

import android.content.Context
import android.text.TextUtils
import com.boardgamegeek.provider.BggContract
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import kotlin.Throws

object FileUtils {
    /**
     * Returns a usable filename from the specified URL.
     */
    fun getFileNameFromUrl(url: String): String? {
        if (!TextUtils.isEmpty(url) && BggContract.INVALID_URL != url) {
            val index = url.lastIndexOf('/')
            if (index > 0) return url.substring(index + 1)
        }
        return null
    }

    /**
     * Find a path to store the specific type of content, ensuring that it exists. Returns null if none can be found or
     * created.
     */
    fun generateContentPath(context: Context?, type: String?): File? {
        if (context == null) {
            return null
        }
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

    fun getExportFileName(type: String): String {
        return "bgg4a-$type.json"
    }
}
