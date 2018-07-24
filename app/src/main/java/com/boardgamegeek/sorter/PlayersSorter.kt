package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.ui.model.Player

abstract class PlayersSorter(context: Context) : Sorter(context) {

    override val defaultSort: String
        get() = PlayPlayers.NAME + BggContract.COLLATE_NOCASE

    abstract fun getSectionText(player: Player): String

    open fun getDisplayText(player: Player): String? = null
}
