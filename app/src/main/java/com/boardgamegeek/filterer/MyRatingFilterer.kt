package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.provider.BggContract.Collection
import java.util.*

class MyRatingFilterer(context: Context) : CollectionFilterer(context) {
    var min = MIN_RANGE
    var max = MAX_RANGE
    var includeUnrated = false

    override val typeResourceId = R.string.collection_filter_type_my_rating

    override fun setData(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toDoubleOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MIN_RANGE
        max = d.getOrNull(1)?.toDoubleOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MAX_RANGE
        includeUnrated = d.getOrNull(2) == "1"
    }

    override fun flatten(): String {
        return "$min$DELIMITER$max$DELIMITER${if (includeUnrated) "1" else "0"}"
    }

    override fun getDisplayText(): String {
        return context.resources.getString(R.string.my_rating_abbr) + " " + describeRange(R.string.unrated_abbr)
    }

    override fun getDescription(): String {
        return context.resources.getString(R.string.my_rating) + " " + describeRange(R.string.unrated)
    }

    private fun describeRange(@StringRes unratedResId: Int): String {
        var text = when (min) {
            max -> String.format(Locale.getDefault(), "%.1f", max)
            else -> String.format(Locale.getDefault(), "%.1f-%.1f", min, max)
        }
        if (includeUnrated) text += String.format(" (+%s)", context.getString(unratedResId))
        return text
    }

    override fun getSelection(): String {
        var format = when (min) {
            max -> "%1\$s=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUnrated) format += " OR %1\$s=0 OR %1\$s IS NULL"
        return String.format(format, Collection.RATING)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when (min) {
            max -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val MIN_RANGE = 1.0
        const val MAX_RANGE = 10.0
    }
}
