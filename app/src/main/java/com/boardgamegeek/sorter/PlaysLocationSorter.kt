package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getString
import com.boardgamegeek.provider.BggContract.Plays

class PlaysLocationSorter(context: Context) : PlaysSorter(context) {
    private val noLocation = context.getString(R.string.no_location)

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_plays_sort_location

    override val type: Int
        get() = PlaysSorterFactory.TYPE_PLAY_LOCATION

    override val sortColumn: String
        get() = Plays.LOCATION

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getString(Plays.LOCATION, noLocation)
    }
}
