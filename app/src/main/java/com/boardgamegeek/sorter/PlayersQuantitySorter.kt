package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.ui.model.Player

class PlayersQuantitySorter(context: Context) : PlayersSorter(context) {

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_sort_quantity

    override val type: Int
        get() = PlayersSorterFactory.TYPE_QUANTITY

    override val sortColumn: String
        get() = Plays.SUM_QUANTITY

    override val isSortDescending: Boolean
        get() = true

    override fun getSectionText(player: Player?): String {
        return (player?.playCount ?: 0).orderOfMagnitude()
    }

    override fun getDisplayText(player: Player): String? = context.resources.getQuantityString(R.plurals.plays_suffix, player.playCount, player.playCount)
}
