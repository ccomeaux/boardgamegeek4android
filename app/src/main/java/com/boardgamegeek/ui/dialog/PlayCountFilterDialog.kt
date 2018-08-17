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

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean): CollectionFilterer {
        val filterer = PlayCountFilterer(context)
        filterer.min = min
        filterer.max = max
        return filterer
    }

    override fun initValues(filter: CollectionFilterer?): SliderFilterDialog.InitialValues {
        val f = filter as PlayCountFilterer?
        return SliderFilterDialog.InitialValues(
                f?.min ?: PlayCountFilterer.lowerBound,
                f?.max ?: PlayCountFilterer.upperBound
        )
    }

    override fun getPinText(context: Context, value: String): String {
        val count = value.toIntOrNull() ?: PlayCountFilterer.lowerBound
        return when (count) {
            PlayCountFilterer.upperBound -> value.andMore()
            else -> value
        }
    }
}
