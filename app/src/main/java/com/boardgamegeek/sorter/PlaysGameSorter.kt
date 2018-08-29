package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getLong
import com.boardgamegeek.extensions.getString
import com.boardgamegeek.provider.BggContract.Plays

class PlaysGameSorter(context: Context) : PlaysSorter(context) {

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_plays_sort_game

    override val type: Int
        get() = PlaysSorterFactory.TYPE_PLAY_GAME

    override val sortColumn: String
        get() = Plays.ITEM_NAME

    override val columns: Array<String>
        get() = arrayOf(Plays.ITEM_NAME, Plays.OBJECT_ID)

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getString(Plays.ITEM_NAME)
    }

    public override fun getHeaderId(cursor: Cursor): Long {
        return cursor.getLong(Plays.OBJECT_ID)
    }
}
