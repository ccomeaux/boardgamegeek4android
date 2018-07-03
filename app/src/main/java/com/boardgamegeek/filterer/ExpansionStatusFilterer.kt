package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.util.StringUtils

class ExpansionStatusFilterer(context: Context) : CollectionFilterer(context) {
    var selectedSubtype = 0

    override val typeResourceId = R.string.collection_filter_type_subtype

    override fun inflate(data: String) {
        selectedSubtype = StringUtils.parseInt(data)
    }

    override fun deflate() = selectedSubtype.toString()

    override fun toShortDescription() = getSelectedFromStringArray(R.array.expansion_status_filter)

    override fun getSelection(): String {
        val value = getSelectedFromStringArray(R.array.expansion_status_filter_values)
        return if (value.isNotEmpty()) Games.SUBTYPE + "=?" else ""
    }

    override fun getSelectionArgs() =
            arrayOf(getSelectedFromStringArray(R.array.expansion_status_filter_values))

    private fun getSelectedFromStringArray(resId: Int): String {
        val values = context.resources.getStringArray(resId)
        return values.getOrNull(selectedSubtype) ?: ""
    }
}
