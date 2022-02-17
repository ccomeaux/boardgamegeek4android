package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.PlayTimeFilterer
import kotlin.math.roundToInt

class PlayTimeFilterDialog : SliderFilterDialog() {
    override fun getType(context: Context) = PlayTimeFilterer(context).type
    override val titleResId = R.string.menu_play_time
    override val descriptionResId = R.string.filter_description_include_missing_play_time
    override val supportsSlider = false
    override val valueFrom = PlayTimeFilterer.lowerBound.toFloat()
    override val valueTo = PlayTimeFilterer.upperBound.toFloat()
    override val stepSize = 5f

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as? PlayTimeFilterer
        return InitialValues(
            (f?.min ?: PlayTimeFilterer.lowerBound).toFloat(),
            (f?.max ?: PlayTimeFilterer.upperBound).toFloat(),
            f?.includeUndefined ?: false,
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return PlayTimeFilterer(context).apply {
            min = low.roundToInt()
            max = high.roundToInt()
            includeUndefined = checkboxIsChecked
        }
    }

    override fun describeRange(context: Context): String {
        return (createFilterer(context) as? PlayTimeFilterer)?.describeRange(" - ")?.ifEmpty { context.getString(R.string.all) } ?: context.getString(
            R.string.all
        )
    }

    override fun formatSliderLabel(context: Context, value: Float): String {
        return when (val time = value.roundToInt()) {
            PlayTimeFilterer.upperBound -> time.asTime().andMore()
            else -> time.asTime()
        }
    }
}
