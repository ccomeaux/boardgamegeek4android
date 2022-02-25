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
        get() = R.drawable.ic_calendar

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toIntOrNull() ?: lowerBound
            max = getOrNull(1)?.toIntOrNull() ?: upperBound
        }
    }

    override fun deflate() = "$min$DELIMITER$max"

    override fun chipText(): String {
        return when {
            min == lowerBound && max == upperBound -> ""
            min == lowerBound -> max.toString().andLess()
            max == upperBound -> min.toString().andMore()
            min == max -> max.toString()
            else -> "$min-$max"
        }
    }

    override fun description(): String {
        @StringRes val prepositionResId: Int
        val year: String
        when {
            min == lowerBound && max == upperBound -> return ""
            min == lowerBound -> {
                prepositionResId = R.string.before
                year = max.toString()
            }
            max == upperBound -> {
                prepositionResId = R.string.after
                year = min.toString()
            }
            min == max -> {
                prepositionResId = R.string.`in`
                year = max.toString()
            }
            else -> {
                prepositionResId = R.string.`in`
                year = "$min - $max"
            }
        }
        return context.getString(R.string.published_prefix, context.getString(prepositionResId), year)
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


