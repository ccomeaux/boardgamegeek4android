package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class PrivateCommentFilter(context: Context) : CollectionTextFilter(context) {
    override val typeResourceId = R.string.collection_filter_type_private_comment

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_3p_24

    override fun chipText() = chipText("")

    override fun description() = description(context.getString(R.string.private_comment))

    override fun filter(item: CollectionItemEntity) = filterByText(item.privateComment)
}
