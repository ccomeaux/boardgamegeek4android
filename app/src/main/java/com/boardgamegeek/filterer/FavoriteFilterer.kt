package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.whereZeroOrNull

class FavoriteFilterer(context: Context) : CollectionFilterer(context) {
    var isFavorite = false

    override val typeResourceId = R.string.collection_filter_type_favorite

    override fun inflate(data: String) {
        isFavorite = data == FAVORITE
    }

    override fun deflate() = if (isFavorite) FAVORITE else NOT_FAVORITE

    override fun toShortDescription() = context.getString(if (isFavorite) R.string.favorites else R.string.not_favorites)
            ?: ""

    override fun getSelection() = when {
        isFavorite -> "${Collection.STARRED}=?"
        else -> Collection.STARRED.whereZeroOrNull()
    }

    override fun getSelectionArgs() = if (isFavorite) arrayOf(FAVORITE) else null

    companion object {
        private const val FAVORITE = "1"
        private const val NOT_FAVORITE = "0"
    }
}
