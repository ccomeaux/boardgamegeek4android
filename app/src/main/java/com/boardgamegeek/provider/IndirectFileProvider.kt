package com.boardgamegeek.provider

import android.content.Context
import android.net.Uri
import com.boardgamegeek.extensions.load
import com.boardgamegeek.util.FileUtils

abstract class IndirectFileProvider : BaseFileProvider() {
    protected abstract fun getFileUri(uri: Uri): Uri?

    protected abstract val columnName: String

    override fun generateFileName(context: Context, uri: Uri): String? {
        return getFileUri(uri)?.let {
            val url = context.contentResolver.load(it, arrayOf(columnName))?.use { cursor ->
                if (cursor.count == 1 && cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
            FileUtils.getFileNameFromUrl(url)
        }
    }
}
