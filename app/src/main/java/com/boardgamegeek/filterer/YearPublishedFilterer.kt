package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.IntervalDelegate
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import java.util.*

class YearPublishedFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)

    override val typeResourceId = R.string.collection_filter_type_year_published

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_calendar_today_24

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toIntOrNull() ?: lowerBound
            max = getOrNull(1)?.toIntOrNull() ?: upperBound
        }
    }

    override fun deflate() = "$min$DELIMITER$max"

    override fun chipText(): String {
        val year = describeRange("-")
        return when {
            min == lowerBound && max == upperBound -> return ""
            min == lowerBound -> year.andLess()
            max == upperBound -> year.andMore()
            else -> year
        }
    }

    override fun description(): String {
        val year: String = describeRange()
        @StringRes val prepositionResId: Int = when {
            min == lowerBound && max == upperBound -> return ""
            min == lowerBound -> R.string.before
            max == upperBound -> R.string.after
            min == max -> R.string.`in`
            else -> R.string.`in`
        }
        return context.getString(R.string.published_prefix, context.getString(prepositionResId), year)
    }

    fun describeRange(rangeSeparator: String = " - "): String {
        return when {
            min == lowerBound && max == upperBound -> return ""
            min == lowerBound -> max.toString()
            max == upperBound -> min.toString()
            min == max -> max.toString()
            else -> "$min$rangeSeparator$max"
        }
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        return when {
            min == lowerBound && max == upperBound -> true
            min == lowerBound -> item.collectionYearPublished <= max
            max == upperBound -> item.collectionYearPublished >= min
            min == max -> item.collectionYearPublished == min
            else -> item.collectionYearPublished in min..max
        }
    }

    companion object {
        const val lowerBound = 1970
        val upperBound = Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}


