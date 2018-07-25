package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.ui.model.Location

class LocationsNameSorter(context: Context) : LocationsSorter(context) {

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_sort_name

    override val type: Int
        get() = LocationsSorterFactory.TYPE_NAME

    override val columns: Array<String>
        get() = arrayOf(Plays.LOCATION)

    override fun getSectionText(location: Location): String {
        return location.name.firstChar()
    }
}
