package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogCollectionFilterRecommendedPlayerCountBinding
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.RecommendedPlayerCountFilterer

class RecommendedPlayerCountFilterDialog : CollectionFilterDialog {
    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        val binding = DialogCollectionFilterRecommendedPlayerCountBinding.inflate(LayoutInflater.from(context))

        val f = filter as RecommendedPlayerCountFilterer?
        val playerCount = f?.playerCount?.coerceIn(binding.rangeBar.tickStart.toInt(), binding.rangeBar.tickEnd.toInt())
                ?: 4
        val recommendation = f?.recommendation ?: RecommendedPlayerCountFilterer.RECOMMENDED

        when (recommendation) {
            RecommendedPlayerCountFilterer.BEST -> binding.bestButton.toggle()
            else -> binding.recommendedButton.toggle()
        }
        binding.rangeBar.setSeekPinByIndex(playerCount - 1)

        val alertDialog = createAlertDialog(context, listener, binding)
        alertDialog.show()
    }

    private fun createAlertDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, binding: DialogCollectionFilterRecommendedPlayerCountBinding): AlertDialog {
        return Builder(context, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.menu_recommended_player_count)
                .setPositiveButton(R.string.set) { _, _ ->
                    if (listener != null) {
                        val filterer = RecommendedPlayerCountFilterer(context)
                        filterer.playerCount = binding.rangeBar.rightIndex + 1
                        filterer.recommendation = if (binding.bestButton.isChecked) RecommendedPlayerCountFilterer.BEST else RecommendedPlayerCountFilterer.RECOMMENDED
                        listener.addFilter(filterer)
                    }
                }
                .setNegativeButton(R.string.clear) { _, _ -> listener?.removeFilter(RecommendedPlayerCountFilterer(context).type) }
                .setView(binding.root)
                .create()
    }

    override fun getType(context: Context) = RecommendedPlayerCountFilterer(context).type
}
