package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getFirstChar
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.provider.BggContract.Plays

class PlayersNameSorter(context: Context) : PlayersSorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.menu_sort_name

    override val type: Int
        get() = PlayersSorterFactory.TYPE_NAME

    override val columns: Array<String>
        get() = arrayOf(PlayPlayers.NAME, PlayPlayers.SUM_QUANTITY)

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getFirstChar(PlayPlayers.NAME)
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        val playCount = cursor.getInt(Plays.SUM_QUANTITY)
        return context.resources.getQuantityString(R.plurals.plays_suffix, playCount, playCount)
    }
}
