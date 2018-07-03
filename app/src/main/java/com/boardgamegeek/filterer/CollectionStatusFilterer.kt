package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import java.util.*

class CollectionStatusFilterer(context: Context) : CollectionFilterer(context) {
    var selectedStatuses: BooleanArray = BooleanArray(0)
    var shouldJoinWithOr: Boolean = false

    override val typeResourceId = R.string.collection_filter_type_collection_status

    override fun setData(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        shouldJoinWithOr = d.getOrNull(0) == "1"
        val ss = BooleanArray(d.size - 1)
        for (i in 0 until d.size - 1) {
            ss[i] = d[i + 1] == "1"
        }
        selectedStatuses = ss
    }

    override fun flatten(): String {
        val sb = StringBuilder(if (shouldJoinWithOr) "1" else "0")
        selectedStatuses.forEach { selected ->
            sb.append(DELIMITER).append(if (selected) "1" else "0")
        }
        return sb.toString()
    }

    override fun getDisplayText(): String {
        val entries = context.resources.getStringArray(R.array.collection_status_filter_entries)
        val displayText = StringBuilder()

        selectedStatuses
                .filter { it }
                .forEachIndexed { i, _ ->
                    if (displayText.isNotEmpty()) displayText.append(if (shouldJoinWithOr) " | " else " & ")
                    displayText.append(entries[i])
                }
        return displayText.toString()
    }

    override fun getDescription(): String {
        return context.getString(R.string.status_of_prefix, super.getDescription())
    }

    override fun getSelection(): String {
        val values = context.resources.getStringArray(R.array.collection_status_filter_values)
        val selection = StringBuilder()

        selectedStatuses
                .filter { it }
                .forEachIndexed { i, _ ->
                    if (selection.isNotEmpty()) selection.append(if (shouldJoinWithOr) " OR " else " AND ")
                    selection.append(values[i]).append("=?")
                }
        return selection.toString()
    }

    override fun getSelectionArgs(): Array<String>? {
        val selectionArgs = ArrayList<String>(selectedStatuses.size)

        selectedStatuses
                .filter { it }
                .forEach { selectionArgs.add("1") }
        return selectionArgs.toTypedArray()
    }
}
