package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.ui.model.Player

class PlayersWinSorter(context: Context) : PlayersSorter(context) {

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_sort_wins

    override val type: Int
        get() = PlayersSorterFactory.TYPE_WINS

    override val sortColumn: String
        get() = Plays.SUM_WINS

    override val isSortDescending: Boolean
        get() = true

    override fun getSectionText(player: Player): String {
        return player.winCount.orderOfMagnitude()
    }

    override fun getDisplayText(player: Player): String? = context.resources.getQuantityString(R.plurals.wins_suffix, player.winCount, player.winCount)
}
