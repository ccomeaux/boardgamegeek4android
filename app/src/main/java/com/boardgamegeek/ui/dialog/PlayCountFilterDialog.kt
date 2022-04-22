package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.PlayCountFilterer
import kotlin.math.roundToInt

class PlayCountFilterDialog : SliderFilterDialog() {
    override fun getType(context: Context) = PlayCountFilterer(context).type
    override val titleResId = R.string.menu_play_count
    override val supportsCheckbox = false
    override val supportsNone = false
    override val valueFrom = PlayCountFilterer.lowerBound.toFloat()
    override val valueTo = PlayCountFilterer.upperBound.toFloat()

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as? PlayCountFilterer
        return InitialValues(
            (f?.min ?: PlayCountFilterer.lowerBound).toFloat(),
            (f?.max ?: PlayCountFilterer.upperBound).toFloat(),
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return PlayCountFilterer(context).apply {
            min = low.roundToInt()
            max = high.roundToInt()
        }
    }

    override fun describeRange(context: Context): String {
        return (createFilterer(context) as? PlayCountFilterer)?.describeRange(" - ").orEmpty()
    }

    override fun formatSliderLabel(context: Context, value: Float): String {
        return when (val count = value.roundToInt()) {
            PlayCountFilterer.upperBound -> count.toString().andMore()
            else -> count.toString()
        }
    }
}
