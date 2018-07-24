package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getFirstChar
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

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getFirstChar(Plays.LOCATION)
    }

    override fun getSectionText(location: Location): String {
        if (location.name.isEmpty()) return "-"
        return location.name.substring(0, 1)
    }
}
