package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.PlayTimeFilterer

class PlayTimeFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_play_time
    override val descriptionResId = R.string.filter_description_include_missing_play_time

    override val absoluteMin = PlayTimeFilterer.lowerBound
    override val absoluteMax = PlayTimeFilterer.upperBound

    override fun getType(context: Context) = PlayTimeFilterer(context).type

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean): CollectionFilterer {
        return PlayTimeFilterer(context).apply {
            this.min = min
            this.max = max
            this.includeUndefined = checkbox
        }
    }

    override fun initValues(filter: CollectionFilterer?): SliderFilterDialog.InitialValues {
        val f = filter as PlayTimeFilterer?
        return SliderFilterDialog.InitialValues(
                f?.min ?: PlayTimeFilterer.lowerBound,
                f?.max ?: PlayTimeFilterer.upperBound,
                f?.includeUndefined ?: false
        )
    }

    override fun getPinText(value: String): String {
        val time = value.toIntOrNull() ?: PlayTimeFilterer.lowerBound
        return when (time) {
            PlayTimeFilterer.upperBound -> "$time+"
            else -> super.getPinText(value)
        }
    }
}
