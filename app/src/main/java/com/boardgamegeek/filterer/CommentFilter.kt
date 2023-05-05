package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class CommentFilter(context: Context) : CollectionTextFilter(context) {
    override val typeResourceId = R.string.collection_filter_type_comment

    override val iconResourceId: Int
        get() = R.drawable.ic_twotone_comment_48

    override fun chipText() = chipText("")

    override fun description() = description(context.getString(R.string.comment))

    override fun filter(item: CollectionItemEntity) = filterByText(item.comment)
}
