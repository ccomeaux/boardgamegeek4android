package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asRating
import com.boardgamegeek.extensions.firstChar

class CollectionNameSorter(context: Context) : CollectionSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_collection_name

    @StringRes
    override val descriptionResId = R.string.collection_sort_collection_name

    override fun getHeaderText(item: CollectionItemEntity) = item.sortName.firstChar()

    override fun getRating(item: CollectionItemEntity): Double = item.averageRating

    override fun getRatingText(item: CollectionItemEntity) = getRating(item).asRating(context, R.string.unrated_abbr)
}
