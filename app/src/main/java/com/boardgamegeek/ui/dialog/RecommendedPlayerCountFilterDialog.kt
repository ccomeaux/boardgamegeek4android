package com.boardgamegeek.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogCollectionFilterRecommendedPlayerCountBinding
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.RecommendedPlayerCountFilterer

class RecommendedPlayerCountFilterDialog : CollectionFilterDialog {
    private var _binding: DialogCollectionFilterRecommendedPlayerCountBinding? = null
    private val binding get() = _binding!!
    private val defaultPlayerCount = 4
    private lateinit var bestString: String
    private lateinit var goodString: String

    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        _binding = DialogCollectionFilterRecommendedPlayerCountBinding.inflate(LayoutInflater.from(context), null, false)

        binding.rangeBar.addOnChangeListener { _, value, _ ->
            binding.playerCountDisplay.text = value.toInt().toString()
        }

        bestString = context.getString(R.string.best)
        goodString = context.getString(R.string.good)

        binding.autocompleteView.setAdapter(ArrayAdapter(context, R.layout.support_simple_spinner_dropdown_item, listOf(bestString, goodString)))

        val recommendedPlayerCountFilterer = filter as? RecommendedPlayerCountFilterer

        when (recommendedPlayerCountFilterer?.recommendation ?: RecommendedPlayerCountFilterer.RECOMMENDED) {
            RecommendedPlayerCountFilterer.BEST -> binding.autocompleteView.setText(bestString, false)
            else -> binding.autocompleteView.setText(goodString, false)
        }

        val playerCount = recommendedPlayerCountFilterer?.playerCount?.coerceIn(binding.rangeBar.valueFrom.toInt(), binding.rangeBar.valueTo.toInt())
            ?: defaultPlayerCount
        binding.rangeBar.value = playerCount.toFloat()

        val alertDialog = createAlertDialog(context, listener)
        alertDialog.show()
    }

    private fun createAlertDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?): AlertDialog {
        return Builder(context, R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.menu_recommended_player_count)
            .setPositiveButton(R.string.set) { _, _ ->
                if (listener != null) {
                    val filterer = RecommendedPlayerCountFilterer(context)
                    filterer.playerCount = binding.rangeBar.value.toInt()
                    filterer.recommendation = if (binding.autocompleteView.text.toString() == bestString)
                        RecommendedPlayerCountFilterer.BEST
                    else
                        RecommendedPlayerCountFilterer.RECOMMENDED
                    listener.addFilter(filterer)
                }
            }
            .setNegativeButton(R.string.clear) { _, _ -> listener?.removeFilter(RecommendedPlayerCountFilterer(context).type) }
            .setView(binding.root)
            .create()
    }

    override fun getType(context: Context) = RecommendedPlayerCountFilterer(context).type
}
