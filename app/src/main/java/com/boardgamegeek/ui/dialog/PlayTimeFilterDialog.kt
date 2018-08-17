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

    override val absoluteMin = PlayTimeFilterer.lowerBound / 5
    override val absoluteMax = PlayTimeFilterer.upperBound / 5

    override fun getType(context: Context) = PlayTimeFilterer(context).type

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean): CollectionFilterer {
        return PlayTimeFilterer(context).apply {
            this.min = min * 5
            this.max = max * 5
            this.includeUndefined = checkbox
        }
    }

    override fun initValues(filter: CollectionFilterer?): SliderFilterDialog.InitialValues {
        val f = filter as PlayTimeFilterer?
        return SliderFilterDialog.InitialValues(
                (f?.min ?: (PlayTimeFilterer.lowerBound * 5)) / 5,
                (f?.max ?: (PlayTimeFilterer.upperBound * 5)) / 5,
                f?.includeUndefined ?: false
        )
    }

    override fun getPinText(context: Context, value: String): String {
        val time = (value.toIntOrNull() ?: PlayTimeFilterer.lowerBound) * 5
        return when (time) {
            PlayTimeFilterer.upperBound -> time.asTime().andMore()
            else -> time.asTime()
        }
    }
}
