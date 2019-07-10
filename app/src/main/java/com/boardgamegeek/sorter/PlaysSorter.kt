package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.provider.BggContract.Plays

abstract class PlaysSorter(context: Context) : Sorter(context) {

    override val defaultSort: String
        get() = Plays.DEFAULT_SORT

    abstract fun getSectionText(play: PlayEntity): String
}