package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.PlayTimeFilterer

class PlayTimeFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_play_time
    override val descriptionResId = R.string.filter_description_include_missing_play_time

    override val absoluteMin = PlayTimeFilterer.lowerBound / tickInterval
    override val absoluteMax = PlayTimeFilterer.upperBound / tickInterval

    override fun getType(context: Context) = PlayTimeFilterer(context).type

    override val supportsSlider = false

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean, ignoreRange: Boolean): CollectionFilterer {
        return PlayTimeFilterer(context).apply {
            this.min = min * tickInterval
            this.max = max * tickInterval
            this.includeUndefined = checkbox
        }
    }

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as PlayTimeFilterer?
        return InitialValues(
                (f?.min ?: (PlayTimeFilterer.lowerBound * tickInterval)) / tickInterval,
                (f?.max ?: (PlayTimeFilterer.upperBound * tickInterval)) / tickInterval,
                f?.includeUndefined ?: false
        )
    }

    override fun getPinText(context: Context, value: String): String {
        return when (val time = (value.toIntOrNull() ?: PlayTimeFilterer.lowerBound) * tickInterval) {
            PlayTimeFilterer.upperBound -> time.asTime().andMore()
            else -> time.asTime()
        }
    }

    companion object {
        private const val tickInterval = 5
    }
}
