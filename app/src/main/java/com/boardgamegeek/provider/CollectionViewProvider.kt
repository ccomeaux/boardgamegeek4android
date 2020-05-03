package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.provider.BggContract.PATH_COLLECTION_VIEWS
import com.boardgamegeek.provider.BggDatabase.Tables

class CollectionViewProvider : BasicProvider() {
    override fun getType(uri: Uri) = CollectionViews.CONTENT_TYPE

    override val path = PATH_COLLECTION_VIEWS

    override val table = Tables.COLLECTION_VIEWS

    override val defaultSortOrder = CollectionViews.DEFAULT_SORT
}
