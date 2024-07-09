package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.RatingFilterer
import java.text.DecimalFormat
import kotlin.math.roundToInt

abstract class RatingFilterDialog<T : RatingFilterer> : SliderFilterDialog() {
    override fun getType(context: Context) = createFilterer(context).type
    override val checkboxTextResId = R.string.unrated
    override val valueFrom = RatingFilterer.LOWER_BOUND.toFloat()
    override val valueTo = RatingFilterer.UPPER_BOUND.toFloat()
    override val stepSize = 0.1f

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        @Suppress("UNCHECKED_CAST")
        val f = filter as? T
        return InitialValues(
            (f?.min ?: RatingFilterer.LOWER_BOUND).toFloat(),
            (f?.max ?: RatingFilterer.UPPER_BOUND).toFloat(),
            f?.includeUndefined ?: false,
            f?.ignoreRange ?: false
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return createTypedFilterer(context).apply {
            min = low.round()
            max = high.round()
            includeUndefined = checkboxIsChecked
            ignoreRange = rangeIsIgnored
        }
    }

    private fun Float.round() = (this * 10).roundToInt().toDouble() / 10.0

    override fun formatSliderLabel(context: Context, value: Float): String {
        return FORMAT.format(value)
    }

    abstract fun createTypedFilterer(context: Context): T

    companion object {
        private val FORMAT = DecimalFormat("#.0")
    }
}
