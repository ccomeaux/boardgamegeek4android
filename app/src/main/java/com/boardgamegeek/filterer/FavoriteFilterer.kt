package com.boardgamegeek.filterer

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.SelectionBuilder

class FavoriteFilterer(context: Context) : CollectionFilterer(context) {
    var isFavorite = false

    override val typeResourceId = R.string.collection_filter_type_favorite

    override fun setData(data: String) {
        isFavorite = data == FAVORITE
    }

    override fun flatten() = if (isFavorite) FAVORITE else NOT_FAVORITE

    override fun getDisplayText() = context.getString(if (isFavorite) R.string.favorites else R.string.not_favorites)!!

    override fun getSelection() = if (isFavorite)
        "${Collection.STARRED}=?"
    else
        SelectionBuilder.whereZeroOrNull(Collection.STARRED)

    override fun getSelectionArgs() = if (isFavorite) arrayOf(FAVORITE) else null

    companion object {
        private const val FAVORITE = "1"
        private const val NOT_FAVORITE = "0"
    }
}
