package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.app.AlertDialog.Builder
import android.view.LayoutInflater
import android.view.View
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.RecommendedPlayerCountFilterer
import kotlinx.android.synthetic.main.dialog_collection_filter_recommended_player_count.view.*

class RecommendedPlayerCountFilterDialog : CollectionFilterDialog {
    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        @SuppressLint("InflateParams")
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_collection_filter_recommended_player_count, null)

        val f = filter as RecommendedPlayerCountFilterer?
        val playerCount = f?.playerCount?.coerceIn(layout.rangeBar.tickStart.toInt(), layout.rangeBar.tickEnd.toInt()) ?: 4
        val recommendation = f?.recommendation ?: RecommendedPlayerCountFilterer.RECOMMENDED

        when (recommendation) {
            RecommendedPlayerCountFilterer.BEST -> layout.bestButton.toggle()
            else -> layout.recommendedButton.toggle()
        }
        layout.rangeBar.setSeekPinByIndex(playerCount - 1)

        val alertDialog = createAlertDialog(context, listener, layout)
        alertDialog.show()
    }

    private fun createAlertDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, layout: View): AlertDialog {
        return Builder(context, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.menu_recommended_player_count)
                .setPositiveButton(R.string.set) { _, _ ->
                    if (listener != null) {
                        val filterer = RecommendedPlayerCountFilterer(context)
                        filterer.playerCount = layout.rangeBar.rightIndex + 1
                        filterer.recommendation = if (layout.bestButton.isChecked) RecommendedPlayerCountFilterer.BEST else RecommendedPlayerCountFilterer.RECOMMENDED
                        listener.addFilter(filterer)
                    }
                }
                .setNegativeButton(R.string.clear) { _, _ -> listener?.removeFilter(RecommendedPlayerCountFilterer(context).type) }
                .setView(layout)
                .create()
    }

    override fun getType(context: Context) = RecommendedPlayerCountFilterer(context).type
}
