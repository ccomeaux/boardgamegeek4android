package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CommentFilter

class CommentFilterDialog : CollectionTextFilterDialog() {
    override val titleResId: Int
        get() = R.string.comment

    override fun createFilter(context: Context) = CommentFilter(context)
}
