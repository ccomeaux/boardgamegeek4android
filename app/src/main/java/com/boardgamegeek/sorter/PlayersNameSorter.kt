package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.ui.model.Player

class PlayersNameSorter(context: Context) : PlayersSorter(context) {

    override val descriptionResId: Int
        @StringRes
        get() = R.string.menu_sort_name

    override val type: Int
        get() = PlayersSorterFactory.TYPE_NAME

    override val columns: Array<String>
        get() = arrayOf(PlayPlayers.NAME, PlayPlayers.SUM_QUANTITY)

    override fun getSectionText(player: Player): String {
        return player.name.firstChar()
    }

    override fun getDisplayText(player: Player): String? = context.resources.getQuantityString(R.plurals.plays_suffix, player.playCount, player.playCount)
}
