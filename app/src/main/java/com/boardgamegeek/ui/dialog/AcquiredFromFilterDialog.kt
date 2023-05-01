package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.AcquiredFromFilter

class AcquiredFromFilterDialog : CollectionTextFilterDialog() {
    override val titleResId: Int
        get() = R.string.acquired_from

    override fun createFilter(context: Context) = AcquiredFromFilter(context)
}
