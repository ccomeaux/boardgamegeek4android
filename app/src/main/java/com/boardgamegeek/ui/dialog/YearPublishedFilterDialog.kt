package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.YearPublishedFilterer
import kotlin.math.roundToInt

class YearPublishedFilterDialog : SliderFilterDialog() {
    override fun getType(context: Context) = YearPublishedFilterer(context).type
    override val titleResId = R.string.menu_year_published
    override val supportsNone = false
    override val supportsCheckbox = false
    override val valueFrom = YearPublishedFilterer.lowerBound.toFloat()
    override val valueTo = YearPublishedFilterer.upperBound.toFloat()

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as? YearPublishedFilterer
        return InitialValues(
            (f?.min ?: YearPublishedFilterer.lowerBound).toFloat(),
            (f?.max ?: YearPublishedFilterer.upperBound).toFloat(),
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return YearPublishedFilterer(context).apply {
            min = low.roundToInt()
            max = high.roundToInt()
        }
    }

    override fun describeRange(context: Context): String {
        return (createFilterer(context) as? YearPublishedFilterer)?.chipText()?.ifBlank { context.getString(R.string.all) }
            ?: context.getString(R.string.all)
    }

    override fun formatSliderLabel(context: Context, value: Float): String {
        return when (val year = value.roundToInt()) {
            YearPublishedFilterer.lowerBound -> year.asYear(context).andLess()
            YearPublishedFilterer.upperBound -> year.asYear(context).andMore()
            else -> year.asYear(context)
        }
    }
}
