package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.SuggestedAgeFilterer

class SuggestedAgeFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_suggested_age
    override val descriptionResId = R.string.filter_description_include_missing_suggested_age

    override val absoluteMin = SuggestedAgeFilterer.lowerBound
    override val absoluteMax = SuggestedAgeFilterer.upperBound

    override fun getType(context: Context) = SuggestedAgeFilterer(context).type

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as SuggestedAgeFilterer?
        return InitialValues(
                f?.min ?: SuggestedAgeFilterer.lowerBound,
                f?.max ?: SuggestedAgeFilterer.upperBound,
                f?.includeUndefined ?: false
        )
    }

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean, ignoreRange: Boolean): CollectionFilterer {
        return SuggestedAgeFilterer(context).apply {
            this.min = min
            this.max = max
            this.includeUndefined = checkbox
        }
    }

    override fun getPinText(context: Context, value: String): String {
        return when (value.toIntOrNull() ?: SuggestedAgeFilterer.lowerBound) {
            SuggestedAgeFilterer.upperBound -> value.andMore()
            else -> value
        }
    }
}
