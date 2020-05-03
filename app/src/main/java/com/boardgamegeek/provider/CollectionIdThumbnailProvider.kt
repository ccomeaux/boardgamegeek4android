package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection

class CollectionIdThumbnailProvider : IndirectFileProvider() {
    override val path = "$PATH_COLLECTION/#/$PATH_THUMBNAILS"

    override val contentPath = PATH_THUMBNAILS

    override val columnName = Collection.COLLECTION_THUMBNAIL_URL

    override fun getFileUri(uri: Uri): Uri? {
        return Collection.buildUri(Collection.getId(uri))
    }
}