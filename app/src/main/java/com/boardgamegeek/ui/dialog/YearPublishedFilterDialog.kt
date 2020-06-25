package com.boardgamegeek.ui.dialog

import android.content.Context
import android.view.View
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.YearPublishedFilterer

class YearPublishedFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_year_published
    override val checkboxVisibility = View.GONE

    override val absoluteMin = YearPublishedFilterer.lowerBound
    override val absoluteMax = YearPublishedFilterer.upperBound

    override fun getType(context: Context) = YearPublishedFilterer(context).type

    override val rangeInterval = 5

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as YearPublishedFilterer?
        return InitialValues(
                f?.min ?: YearPublishedFilterer.lowerBound,
                f?.max ?: YearPublishedFilterer.upperBound
        )
    }

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean, ignoreRange: Boolean): CollectionFilterer {
        return YearPublishedFilterer(context).apply {
            this.min = min
            this.max = max
        }
    }

    override fun getPinText(context: Context, value: String): String {
        return when (val year = value.toIntOrNull() ?: YearPublishedFilterer.lowerBound) {
            YearPublishedFilterer.lowerBound -> year.asYear(context).andLess()
            YearPublishedFilterer.upperBound -> year.asYear(context).andMore()
            else -> year.asYear(context)
        }
    }
}
