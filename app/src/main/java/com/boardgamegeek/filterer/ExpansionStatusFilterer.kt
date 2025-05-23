package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem

class ExpansionStatusFilterer(context: Context) : CollectionFilterer(context) {
    var selectedSubtype = ALL

    override val typeResourceId = R.string.collection_filter_type_subtype

    override fun inflate(data: String) {
        selectedSubtype = data.toIntOrNull() ?: ALL
    }

    override fun deflate() = selectedSubtype.toString()

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_flip_to_back_24

    override fun chipText() = getFromArray(R.array.expansion_status_filter)

    override fun filter(item: CollectionItem): Boolean {
        val value = getFromArray(R.array.expansion_status_filter_values)
        return if (value.isNotEmpty()) {
            (item.subtype == null) ||
            (item.subtype.databaseValue == value)
        } else true
    }

    private fun getFromArray(resId: Int): String {
        return context.resources.getStringArray(resId).getOrNull(selectedSubtype).orEmpty()
    }

    companion object {
        const val ALL = 0
    }
}
