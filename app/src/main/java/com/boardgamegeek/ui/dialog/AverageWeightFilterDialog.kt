package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.AverageWeightFilterer
import com.boardgamegeek.filterer.CollectionFilterer
import java.text.DecimalFormat

class AverageWeightFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_average_weight
    override val descriptionResId = R.string.filter_description_include_missing_average_weight

    override val absoluteMin = (AverageWeightFilterer.lowerBound * FACTOR).toInt()
    override val absoluteMax = (AverageWeightFilterer.upperBound * FACTOR).toInt()

    override fun getType(context: Context) = AverageWeightFilterer(context).type

    override val rangeInterval = 5

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean, ignoreRange: Boolean): CollectionFilterer {
        return AverageWeightFilterer(context).apply {
            this.min = min.toDouble() / FACTOR
            this.max = max.toDouble() / FACTOR
            this.includeUndefined = checkbox
            this.ignoreRange = ignoreRange
        }
    }

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as AverageWeightFilterer?
        return InitialValues(
                ((f?.min ?: AverageWeightFilterer.lowerBound) * FACTOR).toInt(),
                ((f?.max ?: AverageWeightFilterer.upperBound) * FACTOR).toInt(),
                f?.includeUndefined ?: false,
                f?.ignoreRange ?: false
        )
    }

    override fun getPinText(context: Context, value: String): String {
        return FORMAT.format((value.toIntOrNull() ?: 0).toDouble() / FACTOR)
    }

    companion object {
        private const val FACTOR = 10
        private val FORMAT = DecimalFormat("#.0")
    }
}
