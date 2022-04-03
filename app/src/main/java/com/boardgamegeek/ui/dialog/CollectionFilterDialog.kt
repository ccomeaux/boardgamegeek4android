package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.boardgamegeek.filterer.CollectionFilterer

interface CollectionFilterDialog {
    fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?)

    fun getType(context: Context): Int
}
