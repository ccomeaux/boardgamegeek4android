package com.boardgamegeek.ui.dialog

import android.content.Context

import com.boardgamegeek.filterer.CollectionFilterer

interface CollectionFilterDialog {
    interface OnFilterChangedListener {
        fun addFilter(filter: CollectionFilterer)
        fun removeFilter(type: Int)
    }

    fun createDialog(context: Context, listener: OnFilterChangedListener?, filter: CollectionFilterer?)

    fun getType(context: Context): Int
}
