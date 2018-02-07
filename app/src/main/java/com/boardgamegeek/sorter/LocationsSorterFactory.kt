package com.boardgamegeek.sorter

import android.content.Context

class LocationsSorterFactory {
    companion object {
        const val TYPE_NAME = 1
        const val TYPE_QUANTITY = 2
        const val TYPE_DEFAULT = TYPE_NAME

        @JvmStatic
        fun create(context: Context, type: Int): LocationsSorter {
            return when (type) {
                TYPE_QUANTITY -> LocationsQuantitySorter(context)
                TYPE_NAME -> LocationsNameSorter(context)
                else -> LocationsNameSorter(context)
            }
        }
    }
}
