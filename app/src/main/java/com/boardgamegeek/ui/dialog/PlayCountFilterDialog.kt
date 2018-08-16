package com.boardgamegeek.ui.dialog

import android.content.Context
import android.view.View
import com.boardgamegeek.R
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
        var min = PlayCountFilterer.lowerBound
        var max = PlayCountFilterer.upperBound
        if (filter != null) {
            val data = filter as PlayCountFilterer?
            min = data!!.min
            max = data.max
        }
        return SliderFilterDialog.InitialValues(min, max)
    }

    override fun getPinText(value: String): String {
        val count = value.toIntOrNull() ?: PlayCountFilterer.lowerBound
        return when (count) {
            PlayCountFilterer.upperBound -> "$count+"
            else -> super.getPinText(value)
        }
    }
}
