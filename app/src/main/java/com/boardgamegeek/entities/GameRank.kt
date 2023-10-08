package com.boardgamegeek.entities

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getText
import com.boardgamegeek.extensions.toSubtype
import com.boardgamegeek.io.BggService
// TODO remove context-based methods
data class GameRank(
    val type: RankType,
    val name: String = "",
    val friendlyName: String = "",
    val value: Int = RANK_UNKNOWN,
    val bayesAverage: Double = 0.0,
) {
    fun isRankValid() = value != RANK_UNKNOWN

    fun describe(context: Context): CharSequence {
        val subtype = describeType(context)
        return when {
            isRankValid() -> context.getText(R.string.rank_description, value, subtype)
            else -> subtype
        }
    }

    /**
     * Describes the rank with either the subtype or the family name.
     */
    fun describeType(context: Context): CharSequence {
        @StringRes val resId = when (type) {
            RankType.Subtype -> {
                when (name.toSubtype()) {
                    Game.Subtype.BOARDGAME -> R.string.title_board_game
                    Game.Subtype.BOARDGAME_EXPANSION -> R.string.title_expansion
                    Game.Subtype.BOARDGAME_ACCESSORY -> R.string.title_accessory
                    else -> -1
                }
            }
            RankType.Family -> {
                when (name) {
                    BggService.RANK_FAMILY_NAME_ABSTRACT_GAMES -> R.string.title_abstract
                    BggService.RANK_FAMILY_NAME_CHILDRENS_GAMES -> R.string.title_childrens
                    BggService.RANK_FAMILY_NAME_CUSTOMIZABLE_GAMES -> R.string.title_customizable
                    BggService.RANK_FAMILY_NAME_FAMILY_GAMES -> R.string.title_family
                    BggService.RANK_FAMILY_NAME_PARTY_GAMES -> R.string.title_party
                    BggService.RANK_FAMILY_NAME_STRATEGY_GAMES -> R.string.title_strategy
                    BggService.RANK_FAMILY_NAME_THEMATIC_GAMES -> R.string.title_thematic
                    BggService.RANK_FAMILY_NAME_WAR_GAMES -> R.string.title_war
                    else -> -1
                }
            }
        }
        return if (resId == -1) name else context.getText(resId)
    }

    enum class RankType {
        Family,
        Subtype,
    }

    companion object {
        const val RANK_UNKNOWN = Integer.MAX_VALUE
    }
}
