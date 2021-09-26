package com.boardgamegeek.ui.dialog

import android.content.Context
import android.view.View
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.PlayCountFilterer

class PlayCountFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_play_count
    override val checkboxVisibility = View.GONE

    override val absoluteMin = PlayCountFilterer.lowerBound
    override val absoluteMax = PlayCountFilterer.upperBound

    override fun getType(context: Context) = PlayCountFilterer(context).type

    override val rangeInterval = 3

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean, ignoreRange: Boolean): CollectionFilterer {
        val filterer = PlayCountFilterer(context)
        filterer.min = min
        filterer.max = max
        return filterer
    }

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as PlayCountFilterer?
        return InitialValues(
                f?.min ?: PlayCountFilterer.lowerBound,
                f?.max ?: PlayCountFilterer.upperBound
        )
    }

    override fun getPinText(context: Context, value: String): String {
        return when (value.toIntOrNull() ?: PlayCountFilterer.lowerBound) {
            PlayCountFilterer.upperBound -> value.andMore()
            else -> value
        }
    }
}
