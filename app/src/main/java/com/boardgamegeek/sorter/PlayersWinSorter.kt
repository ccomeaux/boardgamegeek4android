package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.Plays

class PlayersWinSorter(context: Context) : PlayersSorter(context) {

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_sort_wins

    override val type: Int
        get() = PlayersSorterFactory.TYPE_WINS

    override val sortColumn: String
        get() = Plays.SUM_WINS

    override val isSortDescending: Boolean
        get() = true

    public override fun getHeaderText(cursor: Cursor): String {
        val quantity = cursor.getInt(Plays.SUM_WINS)
        val prefix = quantity.toString().substring(0, 1)
        val suffix = when {
            quantity >= 10000 -> "0000+"
            quantity >= 1000 -> "000+"
            quantity >= 100 -> "00+"
            quantity >= 10 -> "0+"
            else -> ""
        }
        return prefix + suffix
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        val winCount = cursor.getInt(Plays.SUM_WINS)
        return context.resources.getQuantityString(R.plurals.wins_suffix, winCount, winCount)
    }
}
