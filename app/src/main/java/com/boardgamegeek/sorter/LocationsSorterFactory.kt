package com.boardgamegeek.sorter

import android.content.Context

object LocationsSorterFactory {
    val TYPE_NAME = 1
    val TYPE_QUANTITY = 2
    val TYPE_DEFAULT = TYPE_NAME

    fun create(context: Context, type: Int): LocationsSorter {
        return when (type) {
            TYPE_QUANTITY -> LocationsQuantitySorter(context)
            TYPE_NAME -> LocationsNameSorter(context)
            else -> LocationsNameSorter(context)
        }
    }
}
