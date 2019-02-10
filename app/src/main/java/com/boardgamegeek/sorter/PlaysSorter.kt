package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.ui.model.PlayModel

abstract class PlaysSorter(context: Context) : Sorter(context) {

    override val defaultSort: String
        get() = Plays.DEFAULT_SORT
    
    abstract fun getSectionText(play: PlayModel): String
}