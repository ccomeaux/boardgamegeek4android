package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class CollectionNameFilter(context: Context) : CollectionFilterer(context) {
    var filterText = ""
    var startsWith = false

    override val typeResourceId = R.string.collection_filter_type_collection_name

    override fun inflate(data: String) {
        val lastIndex = data.lastIndexOf(DELIMITER)
        if (lastIndex == -1) {
            filterText = data
            startsWith = false
        } else {
            filterText = data.substring(0, lastIndex)
            startsWith = data.substring(lastIndex + 1) == "1"
        }
    }

    override fun deflate() = "$filterText$DELIMITER${if (startsWith) "1" else "0"}"

    override fun toShortDescription() = if (startsWith) "$filterText*" else "*$filterText*"

    override fun toLongDescription() = context.getString(if (startsWith) R.string.starts_with_prefix else R.string.named_prefix, filterText)
            ?: ""

    override fun getSelection() = "${Collection.COLLECTION_NAME} LIKE ?"

    override fun getSelectionArgs() = if (startsWith) arrayOf("$filterText%") else arrayOf("%$filterText%")
}
