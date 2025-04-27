package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.AverageWeightFilterer
import com.boardgamegeek.filterer.CollectionFilterer
import java.text.DecimalFormat
import kotlin.math.roundToInt

class AverageWeightFilterDialog : SliderFilterDialog() {
    override fun getType(context: Context) = AverageWeightFilterer(context).type
    override val titleResId = R.string.menu_average_weight
    override val descriptionResId = R.string.filter_description_include_missing_average_weight
    override val valueFrom = AverageWeightFilterer.LOWER_BOUND.toFloat()
    override val valueTo = AverageWeightFilterer.UPPER_BOUND.toFloat()
    override val stepSize = 0.1f

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as? AverageWeightFilterer
        return InitialValues(
            (f?.min ?: AverageWeightFilterer.LOWER_BOUND).toFloat(),
            (f?.max ?: AverageWeightFilterer.UPPER_BOUND).toFloat(),
            f?.includeUndefined ?: false,
            f?.ignoreRange ?: false,
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return AverageWeightFilterer(context).apply {
            min = low.round()
            max = high.round()
            includeUndefined = checkboxIsChecked
            ignoreRange = rangeIsIgnored
        }
    }

    override fun describeRange(context: Context): String {
        return (createFilterer(context) as? AverageWeightFilterer)?.describeRange(" - ").orEmpty()
    }

    override fun formatSliderLabel(context: Context, value: Float): String {
        return FORMAT.format(value)
    }

    private fun Float.round() = (this * 10).roundToInt().toDouble() / 10.0

    companion object {
        private val FORMAT = DecimalFormat("#.0")
    }
}
