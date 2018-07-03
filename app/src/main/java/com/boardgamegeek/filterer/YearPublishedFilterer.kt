package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class YearPublishedFilterer(context: Context) : CollectionFilterer(context) {
    var min = MIN_RANGE
    var max = MAX_RANGE

    override val typeResourceId = R.string.collection_filter_type_year_published

    override fun setData(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MIN_RANGE
        max = d.getOrNull(1)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MAX_RANGE
    }

    override fun flatten() = "$min$DELIMITER$max"

    override fun getDisplayText(): String {
        return when {
            min == MIN_RANGE && max == MAX_RANGE -> ""
            min == MIN_RANGE -> "$max-"
            max == MAX_RANGE -> "$min+"
            min == max -> max.toString()
            else -> "$min-$max"
        }
    }

    override fun getDescription(): String {
        @StringRes val prepositionResId: Int
        val year: String
        when {
            min == MIN_RANGE && max == MAX_RANGE -> return ""
            min == MIN_RANGE -> {
                prepositionResId = R.string.before
                year = (max + 1).toString()
            }
            max == MAX_RANGE -> {
                prepositionResId = R.string.after
                year = (min - 1).toString()
            }
            min == max -> {
                prepositionResId = R.string.`in`
                year = max.toString()
            }
            else -> {
                prepositionResId = R.string.`in`
                year = "$min-$max"
            }
        }
        return context.getString(R.string.published_prefix, context.getString(prepositionResId), year)
    }

    override fun getSelection(): String {
        val format = when {
            min == MIN_RANGE && max == MAX_RANGE -> ""
            min == MIN_RANGE -> "%1\$s<=?"
            max == MAX_RANGE -> "%1\$s>=?"
            min == max -> "%1\$s=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        return String.format(format, Games.YEAR_PUBLISHED)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when {
            min == MIN_RANGE && max == MAX_RANGE -> null
            min == MIN_RANGE -> arrayOf(max.toString())
            max == MAX_RANGE -> arrayOf(min.toString())
            min == max -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val MIN_RANGE = 1970
        @JvmStatic
        val MAX_RANGE = Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}


