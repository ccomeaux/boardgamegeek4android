package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor

import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.PlayPlayers

abstract class PlayersSorter(context: Context) : Sorter(context) {

    override val defaultSort: String
        get() = PlayPlayers.NAME + BggContract.COLLATE_NOCASE

    open fun getDisplayInfo(cursor: Cursor): String {
        return getHeaderText(cursor)
    }
}