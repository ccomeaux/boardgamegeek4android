package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

abstract class SuggestedAgeSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.getString(R.string.text_unknown)

    @StringRes
    override val descriptionResId = R.string.collection_sort_suggested_age

    override fun getHeaderText(item: CollectionItemEntity): String {
        return if (item.minimumAge == 0) defaultValue else item.minimumAge.toString()
    }

    override fun getDisplayInfo(item: CollectionItemEntity): String {
        val info = getHeaderText(item)
        return when {
            defaultValue != info -> "${context.getString(R.string.ages)} $info+"
            else -> "${context.getString(R.string.ages)} $info"
        }
    }
}
