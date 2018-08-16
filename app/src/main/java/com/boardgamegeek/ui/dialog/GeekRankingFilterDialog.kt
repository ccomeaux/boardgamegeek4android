package com.boardgamegeek.ui.dialog

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.GeekRankingFilterer

class GeekRankingFilterDialog : SliderFilterDialog() {
    override val titleResId = R.string.menu_geek_ranking

    override val absoluteMin = GeekRankingFilterer.lowerBound
    override val absoluteMax = GeekRankingFilterer.upperBound

    override fun getType(context: Context) = GeekRankingFilterer(context).type

    override fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean): CollectionFilterer {
        return GeekRankingFilterer(context).apply {
            this.min = min
            this.max = max
            this.includeUnranked = checkbox
        }
    }

    override fun initValues(filter: CollectionFilterer?): SliderFilterDialog.InitialValues {
        val f = filter as GeekRankingFilterer?
        return SliderFilterDialog.InitialValues(
                f?.min ?: GeekRankingFilterer.lowerBound,
                f?.max ?: GeekRankingFilterer.upperBound,
                f?.includeUnranked ?: false
        )
    }
}
