package com.boardgamegeek.sorter

import android.content.Context

import com.boardgamegeek.provider.BggContract.Plays

abstract class PlaysSorter(context: Context) : Sorter(context) {

    override val defaultSort: String
        get() = Plays.DEFAULT_SORT
}