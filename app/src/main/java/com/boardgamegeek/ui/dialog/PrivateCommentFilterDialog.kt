package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.PrivateCommentFilter

class PrivateCommentFilterDialog : CollectionTextFilterDialog() {
    override val titleResId: Int
        get() = R.string.private_comment

    override fun createFilter(context: Context) = PrivateCommentFilter(context)
}
