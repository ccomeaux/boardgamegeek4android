package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.PlayerNumberFilterer
import kotlin.math.roundToInt

class PlayerNumberFilterDialog : SliderFilterDialog() {
    override fun getType(context: Context) = PlayerNumberFilterer(context).type
    override val titleResId = R.string.menu_number_of_players
    override val descriptionResId = R.string.filter_description_player_number
    override val checkboxTextResId = R.string.exact
    override val valueFrom = PlayerNumberFilterer.LOWER_BOUND.toFloat()
    override val valueTo = PlayerNumberFilterer.UPPER_BOUND.toFloat()
    override val supportsNone = false

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as? PlayerNumberFilterer
        return InitialValues(
            (f?.min ?: 4).toFloat(),
            (f?.max ?: 4).toFloat(),
            f?.isExact ?: false,
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return PlayerNumberFilterer(context).apply {
            min = low.roundToInt()
            max = high.roundToInt()
            isExact = checkboxIsChecked
        }
    }

    override fun describeRange(context: Context): String {
        return (createFilterer(context) as? PlayerNumberFilterer)?.describeRange(" - ").orEmpty()
    }
}
