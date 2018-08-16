package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.RatingFilterer
import java.text.DecimalFormat

abstract class RatingFilterDialog<T : RatingFilterer> : SliderFilterDialog() {
    override val checkboxTextResId = R.string.unrated

    override val absoluteMin = (RatingFilterer.lowerBound * FACTOR).toInt()
    override val absoluteMax = (RatingFilterer.upperBound * FACTOR).toInt()

    override fun getType(context: Context) = createFilterer(context).type

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean): CollectionFilterer {
        val x = createFilterer(context)
        return x.apply {
            this.min = min.toDouble() / FACTOR
            this.max = max.toDouble() / FACTOR
            includeUndefined = checkbox
        }
    }

    abstract fun createFilterer(context: Context): T

    override fun initValues(filter: CollectionFilterer?): SliderFilterDialog.InitialValues {
        @Suppress("UNCHECKED_CAST")
        val f = filter as T?
        return SliderFilterDialog.InitialValues(
                ((f?.min ?: RatingFilterer.lowerBound) * FACTOR).toInt(),
                ((f?.max ?: RatingFilterer.upperBound) * FACTOR).toInt(),
                f?.includeUndefined ?: true
        )
    }

    override fun getPinText(value: String): String {
        return FORMAT.format((value.toIntOrNull() ?: 0).toDouble() / FACTOR)
    }

    companion object {
        private const val FACTOR = 10
        private val FORMAT = DecimalFormat("#.0")
    }
}
