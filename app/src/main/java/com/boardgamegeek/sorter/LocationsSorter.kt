package com.boardgamegeek.sorter

import android.content.Context

import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Plays

abstract class LocationsSorter(context: Context) : Sorter(context) {

    override val defaultSort: String
        get() = Plays.LOCATION + BggContract.COLLATE_NOCASE
}