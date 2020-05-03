package com.boardgamegeek.provider

import android.content.Context
import android.net.Uri
import com.boardgamegeek.provider.BggContract.PATH_THUMBNAILS

class ThumbnailsIdProvider : BaseFileProvider() {
    override val path = "$PATH_THUMBNAILS/*"

    override val contentPath = PATH_THUMBNAILS

    override fun generateFileName(context: Context, uri: Uri): String? {
        return uri.lastPathSegment
    }
}