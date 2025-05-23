package com.boardgamegeek.model

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getText

data class GameSubtype(
    val subtype: Game.Subtype,
    val rank: Int = RANK_UNKNOWN,
    val bayesAverage: Double = 0.0,
) {
    fun isRankValid() = rank != RANK_UNKNOWN

    fun describe(context: Context): CharSequence {
        val typeDescription = describeType(context)
        return when {
            isRankValid() -> context.getText(R.string.rank_description, rank, typeDescription)
            else -> typeDescription
        }
    }

    /**
     * Describes the rank with either the subtype or the family name.
     */
    fun describeType(context: Context): CharSequence {
        @StringRes val resId = when (subtype) {
            Game.Subtype.BoardGame -> R.string.title_board_game
            Game.Subtype.BoardGameExpansion -> R.string.title_expansion
            Game.Subtype.BoardGameAccessory -> R.string.title_accessory
            else -> 0
        }
        return if (resId == 0) "" else context.getText(resId)
    }

    companion object {
        const val RANK_UNKNOWN = Integer.MAX_VALUE
    }
}
