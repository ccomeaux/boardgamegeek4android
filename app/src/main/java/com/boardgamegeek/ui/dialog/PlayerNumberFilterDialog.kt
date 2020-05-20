package com.boardgamegeek.ui.dialog

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.PlayerNumberFilterer

class PlayerNumberFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_number_of_players
    override val descriptionResId = R.string.filter_description_player_number
    override val checkboxTextResId = R.string.exact

    override val absoluteMin = PlayerNumberFilterer.lowerBound
    override val absoluteMax = PlayerNumberFilterer.upperBound

    override val supportsNone: Boolean
        get() = false

    override fun getType(context: Context) = PlayerNumberFilterer(context).type

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean, ignoreRange: Boolean): CollectionFilterer {
        return PlayerNumberFilterer(context).apply {
            this.min = min
            this.max = max
            this.isExact = checkbox
        }
    }

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as PlayerNumberFilterer?
        return InitialValues(
                f?.min ?: 4,
                f?.max ?: 4,
                f?.isExact ?: false
        )
    }
}
