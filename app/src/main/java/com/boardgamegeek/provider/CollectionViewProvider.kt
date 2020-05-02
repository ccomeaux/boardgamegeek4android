package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.provider.BggContract.PATH_COLLECTION_VIEWS
import com.boardgamegeek.provider.BggDatabase.Tables

class CollectionViewProvider : BasicProvider() {
    public override fun getType(uri: Uri) = CollectionViews.CONTENT_TYPE

    override fun getPath() = PATH_COLLECTION_VIEWS

    override val table = Tables.COLLECTION_VIEWS

    override fun getDefaultSortOrder() = CollectionViews.DEFAULT_SORT
}
