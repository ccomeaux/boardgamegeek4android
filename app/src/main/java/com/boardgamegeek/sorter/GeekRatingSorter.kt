package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.text.DecimalFormat

class GeekRatingSorter(context: Context) : RatingSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_geek_rating

    @StringRes
    override val descriptionResId = R.string.collection_sort_geek_rating

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.geekRating }

    override val displayFormat = DecimalFormat("0.000")

    override fun getRating(item: CollectionItemEntity) = item.geekRating
}
