package com.boardgamegeek.sorter

import android.content.Context

class PlayersSorterFactory {

    companion object {
        const val TYPE_NAME = 1
        const val TYPE_QUANTITY = 2
        const val TYPE_WINS = 3
        const val TYPE_DEFAULT = TYPE_NAME

        @JvmStatic
        fun create(context: Context, type: Int): PlayersSorter {
            return when (type) {
                TYPE_QUANTITY -> PlayersQuantitySorter(context)
                TYPE_WINS -> PlayersWinSorter(context)
                TYPE_NAME -> PlayersNameSorter(context)
                else -> PlayersNameSorter(context)
            }
        }
    }
}
