package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.ui.model.Location

class LocationsQuantitySorter(context: Context) : LocationsSorter(context) {

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_sort_quantity

    override val type: Int
        get() = LocationsSorterFactory.TYPE_QUANTITY

    override val sortColumn: String
        get() = Plays.SUM_QUANTITY

    override val isSortDescending: Boolean
        get() = true

    override fun getSectionText(location: Location?): String {
        return (location?.playCount ?: 0).orderOfMagnitude()
    }
}
