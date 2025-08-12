package com.boardgamegeek.model

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getText

data class GameFamily(
    val family: Family,
    val rank: Int = Game.RANK_UNKNOWN,
    val bayesAverage: Double = 0.0,
) {
    enum class Family {
        Abstract,
        Customizable,
        Childrens,
        Family,
        Party,
        Strategy,
        Thematic,
        War,
        Unknown,
    }

    fun isRankValid() = rank != Game.RANK_UNKNOWN

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
    fun describeType(context: Context): String {
        @StringRes val resId = when (family) {
            Family.Abstract -> R.string.title_abstract
            Family.Childrens -> R.string.title_childrens
            Family.Customizable -> R.string.title_customizable
            Family.Family -> R.string.title_family
            Family.Party -> R.string.title_party
            Family.Strategy -> R.string.title_strategy
            Family.Thematic -> R.string.title_thematic
            Family.War -> R.string.title_war
            Family.Unknown -> 0
        }
        return if (resId == 0) "" else context.getString(resId)
    }
}
