package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
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

    override fun getSectionText(play: PlayEntity): String {
        return play.gameName
    }
}
