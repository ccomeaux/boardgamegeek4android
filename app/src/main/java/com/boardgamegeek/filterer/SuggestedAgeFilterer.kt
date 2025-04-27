package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.IntervalDelegate
import java.util.*

class SuggestedAgeFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(LOWER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var max by IntervalDelegate(UPPER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var includeUndefined = false
    var ignoreRange = false

    override val typeResourceId = R.string.collection_filter_type_suggested_age

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toIntOrNull() ?: LOWER_BOUND
            max = getOrNull(1)?.toIntOrNull() ?: UPPER_BOUND
            includeUndefined = getOrNull(2) == "1"
            ignoreRange = getOrNull(3) == "1"
        }
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}$DELIMITER${if (ignoreRange) "1" else "0"}"

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_supervisor_account_24

    override fun chipText(): String {
        val range = describeRange(R.string.and_up_suffix_abbr)
        return when {
            ignoreRange && includeUndefined -> context.getString(R.string.unknown_abbr)
            includeUndefined -> "$range (+${context.getString(R.string.unknown_abbr)})"
            else -> range
        }
    }

    override fun description(): String {
        val range = describeRange(R.string.and_up_suffix_abbr)
        return "${context.getString(R.string.ages)} " + when {
            ignoreRange && includeUndefined -> context.getString(R.string.unknown_abbr)
            includeUndefined -> "$range (+${context.getString(R.string.unknown_abbr)})"
            else -> range
        }
    }

    fun describeRange(@StringRes andUpResId: Int = R.string.and_up_suffix_abbr, rangeSeparator: String = "-"): String {
        val range = when {
            ignoreRange -> ""
            max == UPPER_BOUND -> context.getString(andUpResId, min)
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            else -> String.format(Locale.getDefault(), "%,d$rangeSeparator%,d", min, max)
        }
        return range
    }

    override fun filter(item: CollectionItem): Boolean {
        return when {
            ignoreRange -> false
            max == UPPER_BOUND -> item.minimumAge >= min
            else -> item.minimumAge in min..max
        } || if (includeUndefined) item.minimumAge == 0 else false
    }

    companion object {
        const val LOWER_BOUND = 1
        const val UPPER_BOUND = 21
    }
}
