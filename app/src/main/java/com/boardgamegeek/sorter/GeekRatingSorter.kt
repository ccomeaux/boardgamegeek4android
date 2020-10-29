package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Collection
import java.text.DecimalFormat

class GeekRatingSorter(context: Context) : RatingSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_geek_rating

    @StringRes
    override val descriptionResId = R.string.collection_sort_geek_rating

    override val sortColumn = Collection.STATS_BAYES_AVERAGE

    override val displayFormat = DecimalFormat("0.000")

    override fun getRating(item: CollectionItemEntity) = item.geekRating
}
