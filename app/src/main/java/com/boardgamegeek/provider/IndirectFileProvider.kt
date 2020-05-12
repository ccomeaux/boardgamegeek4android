package com.boardgamegeek.provider

import android.content.Context
import android.net.Uri
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.util.ResolverUtils

abstract class IndirectFileProvider : BaseFileProvider() {
    protected abstract fun getFileUri(uri: Uri): Uri?

    protected abstract val columnName: String

    override fun generateFileName(context: Context, uri: Uri): String? {
        val url = ResolverUtils.queryString(context.contentResolver, getFileUri(uri), columnName)
        return FileUtils.getFileNameFromUrl(url)
    }
}