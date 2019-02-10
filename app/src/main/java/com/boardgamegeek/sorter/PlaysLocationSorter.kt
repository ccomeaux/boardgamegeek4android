package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.ui.model.PlayModel

class PlaysLocationSorter(context: Context) : PlaysSorter(context) {
    private val noLocation = context.getString(R.string.no_location)

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_plays_sort_location

    override val type: Int
        get() = PlaysSorterFactory.TYPE_PLAY_LOCATION

    override val sortColumn: String
        get() = Plays.LOCATION

    override fun getSectionText(play: PlayModel): String {
        if (play.location.isBlank()) return noLocation
        return play.location
    }
}
