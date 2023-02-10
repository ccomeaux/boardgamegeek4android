package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.text.SimpleDateFormat
import java.util.*

abstract class CollectionDateSorter(context: Context) : CollectionSorter(context) {
    private val headerDateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    protected open val defaultValueResId: Int
        @StringRes
        get() = R.string.text_unknown

    override fun getHeaderText(item: CollectionItemEntity): String {
        val time =  getTimestamp(item)
        return if (time == 0L) context.getString(defaultValueResId) else headerDateFormat.format(time)
    }

    override fun getDisplayInfo(item: CollectionItemEntity) = ""
}
