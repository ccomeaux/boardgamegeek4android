package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.provider.BggContract.Collection
import java.text.DecimalFormat

class MyRatingSorter(context: Context) : RatingSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_my_rating

    @StringRes
    override val descriptionResId = R.string.collection_sort_my_rating

    override val sortColumn = Collection.RATING

    override val displayFormat = DecimalFormat("0.0")

    override fun getRating(item: CollectionItemEntity) = item.rating

    override fun getRatingText(item: CollectionItemEntity) = getRating(item).asPersonalRating(context, R.string.unrated_abbr)
}
