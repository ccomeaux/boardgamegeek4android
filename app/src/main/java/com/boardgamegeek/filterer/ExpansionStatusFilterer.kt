package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class ExpansionStatusFilterer(context: Context) : CollectionFilterer(context) {
    var selectedSubtype = ALL

    override val typeResourceId = R.string.collection_filter_type_subtype

    override fun inflate(data: String) {
        selectedSubtype = data.toIntOrNull() ?: ALL
    }

    override fun deflate() = selectedSubtype.toString()

    override fun toShortDescription() = getFromArray(R.array.expansion_status_filter)

    override fun filter(item: CollectionItemEntity): Boolean {
        val value = getFromArray(R.array.expansion_status_filter_values)
        return if (value.isNotEmpty()) item.subType == value else true
    }

    private fun getFromArray(resId: Int): String {
        return context.resources.getStringArray(resId).getOrNull(selectedSubtype) ?: ""
    }

    companion object {
        const val ALL = 0
    }
}
