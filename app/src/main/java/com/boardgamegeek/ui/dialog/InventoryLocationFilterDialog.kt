package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.InventoryLocationFilter

class InventoryLocationFilterDialog : CollectionTextFilterDialog() {
    override val titleResId: Int
        get() = R.string.acquired_from

    override fun createFilter(context: Context) = InventoryLocationFilter(context)
}
