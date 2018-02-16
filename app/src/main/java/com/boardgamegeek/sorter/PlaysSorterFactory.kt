package com.boardgamegeek.sorter

import android.content.Context

class PlaysSorterFactory {
    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_PLAY_DATE = 1
        const val TYPE_PLAY_LOCATION = 2
        const val TYPE_PLAY_GAME = 3
        const val TYPE_PLAY_LENGTH = 4
        const val TYPE_DEFAULT = TYPE_PLAY_DATE

        @JvmStatic
        fun create(context: Context, type: Int): PlaysSorter? {
            return when (type) {
                TYPE_PLAY_DATE -> PlaysDateSorter(context)
                TYPE_PLAY_LOCATION -> PlaysLocationSorter(context)
                TYPE_PLAY_GAME -> PlaysGameSorter(context)
                TYPE_PLAY_LENGTH -> PlaysLengthSorter(context)
                TYPE_UNKNOWN -> null
                else -> null
            }
        }
    }
}
