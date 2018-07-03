package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class YearPublishedFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)

    override val typeResourceId = R.string.collection_filter_type_year_published

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
    }

    override fun deflate() = "$min$DELIMITER$max"

    override fun toShortDescription(): String {
        return when {
            min == lowerBound && max == upperBound -> ""
            min == lowerBound -> "$max-"
            max == upperBound -> "$min+"
            min == max -> max.toString()
            else -> "$min-$max"
        }
    }

    override fun toLongDescription(): String {
        @StringRes val prepositionResId: Int
        val year: String
        when {
            min == lowerBound && max == upperBound -> return ""
            min == lowerBound -> {
                prepositionResId = R.string.before
                year = (max + 1).toString()
            }
            max == upperBound -> {
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
            min == lowerBound && max == upperBound -> ""
            min == lowerBound -> "%1\$s<=?"
            max == upperBound -> "%1\$s>=?"
            min == max -> "%1\$s=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        return String.format(format, Games.YEAR_PUBLISHED)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when {
            min == lowerBound && max == upperBound -> null
            min == lowerBound -> arrayOf(max.toString())
            max == upperBound -> arrayOf(min.toString())
            min == max -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val lowerBound = 1970
        @JvmStatic
        val upperBound = Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}


