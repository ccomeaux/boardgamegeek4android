package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.SuggestedAgeFilterer
import kotlin.math.roundToInt

class SuggestedAgeFilterDialog : SliderFilterDialog() {
    override fun getType(context: Context) = SuggestedAgeFilterer(context).type
    override val titleResId = R.string.menu_suggested_age
    override val descriptionResId = R.string.filter_description_include_missing_suggested_age
    override val valueFrom = SuggestedAgeFilterer.lowerBound.toFloat()
    override val valueTo = SuggestedAgeFilterer.upperBound.toFloat()

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as? SuggestedAgeFilterer
        return InitialValues(
            (f?.min ?: SuggestedAgeFilterer.lowerBound).toFloat(),
            (f?.max ?: SuggestedAgeFilterer.upperBound).toFloat(),
            f?.includeUndefined ?: false,
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return SuggestedAgeFilterer(context).apply {
            min = low.roundToInt()
            max = high.roundToInt()
            includeUndefined = checkboxIsChecked
        }
    }

    override fun describeRange(context: Context): String {
        return (createFilterer(context) as? SuggestedAgeFilterer)?.describeRange(rangeSeparator = " - ") ?: context.getString(R.string.all)
    }

    override fun formatSliderLabel(context: Context, value: Float): String {
        return when (val age = value.roundToInt()) {
            SuggestedAgeFilterer.upperBound -> age.toString().andMore()
            else -> age.toString()
        }
    }
}
