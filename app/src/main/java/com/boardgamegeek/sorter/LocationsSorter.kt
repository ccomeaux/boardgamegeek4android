package com.boardgamegeek.sorter

import android.content.Context

import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.ui.model.Location

abstract class LocationsSorter(context: Context) : Sorter(context) {

    override val defaultSort: String
        get() = Plays.LOCATION + BggContract.COLLATE_NOCASE

    abstract fun getSectionText(location: Location?): String
}