package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS

class CollectionIdThumbnailProvider : IndirectFileProvider() {
    override val path = "$PATH_COLLECTION/#/$PATH_THUMBNAILS"

    override val contentPath = PATH_THUMBNAILS

    override val columnName = Collection.Columns.COLLECTION_THUMBNAIL_URL

    override fun getFileUri(uri: Uri) = Collection.buildUri(Collection.getId(uri))
}
