package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.provider.BggContract.Collection
import java.text.DecimalFormat

class MyRatingSorter(context: Context) : RatingSorter(context) {
    private val format = DecimalFormat("0.0")

    @StringRes
    override val descriptionResId = R.string.collection_sort_my_rating

    @StringRes
    public override val typeResId = R.string.collection_sort_type_my_rating

    override val sortColumn = Collection.RATING

    override val displayFormat = format

    override fun getRatingText(cursor: Cursor) = getRating(cursor).asPersonalRating(context, R.string.unrated_abbr)
}
