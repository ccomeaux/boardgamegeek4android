package com.boardgamegeek.filterer

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class CollectionNameFilter(context: Context) : CollectionFilterer(context) {
    var filterText = ""
    var startsWith = false

    override val typeResourceId = R.string.collection_filter_type_collection_name

    override fun setData(data: String) {
        val lastIndex = data.lastIndexOf(DELIMITER)
        if (lastIndex == -1) {
            filterText = data
            startsWith = false
        } else {
            filterText = data.substring(0, lastIndex)
            startsWith = data.substring(lastIndex) == "1"
        }
    }

    override fun flatten(): String {
        return "$filterText$DELIMITER${if (startsWith) "1" else "0"}"
    }

    override fun getDisplayText(): String {
        return if (startsWith) "$filterText*" else "*$filterText*"
    }

    override fun getDescription(): String {
        return if (startsWith) context.getString(R.string.starts_with_prefix, filterText) else context.getString(R.string.named_prefix, filterText)
    }

    override fun getSelection(): String {
        return "${Collection.COLLECTION_NAME} LIKE ?"
    }

    override fun getSelectionArgs(): Array<String>? {
        return if (startsWith) arrayOf("$filterText%") else arrayOf("%$filterText%")
    }
}
