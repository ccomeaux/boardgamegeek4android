package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem

class CollectionNameFilter(context: Context) : CollectionFilterer(context) {
    var filterText = ""
    var startsWith = false

    override val typeResourceId = R.string.collection_filter_type_collection_name

    override fun inflate(data: String) {
        filterText = data.substringBeforeLast(DELIMITER)
        startsWith = data.substringAfterLast(DELIMITER) == "1"
    }

    override fun deflate() = "$filterText$DELIMITER${if (startsWith) "1" else "0"}"

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_format_quote_24

    override fun chipText() = if (startsWith) "$filterText*" else "*$filterText*"

    override fun description() = context.getString(if (startsWith) R.string.starts_with_prefix else R.string.named_prefix, filterText)

    override fun filter(item: CollectionItem): Boolean {
        return if (startsWith) {
            item.collectionName.startsWith(filterText, true)
        } else {
            item.collectionName.contains(filterText, true)
        }
    }
}
