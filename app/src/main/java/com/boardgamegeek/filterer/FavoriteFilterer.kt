package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class FavoriteFilterer(context: Context) : CollectionFilterer(context) {
    var isFavorite = false

    override val typeResourceId = R.string.collection_filter_type_favorite

    override fun inflate(data: String) {
        isFavorite = data == FAVORITE
    }

    override fun deflate() = if (isFavorite) FAVORITE else NOT_FAVORITE

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_favorite_24

    override fun chipText() = context.getString(if (isFavorite) R.string.favorites else R.string.not_favorites)

    override fun filter(item: CollectionItemEntity): Boolean {
        return item.isFavorite == isFavorite
    }

    companion object {
        private const val FAVORITE = "1"
        private const val NOT_FAVORITE = "0"
    }
}
