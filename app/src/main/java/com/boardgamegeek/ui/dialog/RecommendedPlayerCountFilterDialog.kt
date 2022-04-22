package com.boardgamegeek.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogCollectionFilterRecommendedPlayerCountBinding
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.RecommendedPlayerCountFilterer
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

class RecommendedPlayerCountFilterDialog : CollectionFilterDialog {
    private var _binding: DialogCollectionFilterRecommendedPlayerCountBinding? = null
    private val binding get() = _binding!!
    private val defaultPlayerCount = 4

    override fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?) {
        val viewModel by lazy { ViewModelProvider(activity)[CollectionViewViewModel::class.java] }
        _binding = DialogCollectionFilterRecommendedPlayerCountBinding.inflate(LayoutInflater.from(activity), null, false)

        binding.rangeBar.addOnChangeListener { _, value, _ ->
            binding.playerCountDisplay.text = value.toInt().toString()
        }

        val bestString = activity.getString(R.string.best)
        val goodString = activity.getString(R.string.good)

        binding.autocompleteView.setAdapter(ArrayAdapter(activity, R.layout.support_simple_spinner_dropdown_item, listOf(bestString, goodString)))

        val recommendedPlayerCountFilterer = filter as? RecommendedPlayerCountFilterer

        when (recommendedPlayerCountFilterer?.recommendation ?: RecommendedPlayerCountFilterer.RECOMMENDED) {
            RecommendedPlayerCountFilterer.BEST -> binding.autocompleteView.setText(bestString, false)
            else -> binding.autocompleteView.setText(goodString, false)
        }

        val playerCount = recommendedPlayerCountFilterer?.playerCount?.coerceIn(binding.rangeBar.valueFrom.toInt(), binding.rangeBar.valueTo.toInt())
            ?: defaultPlayerCount
        binding.rangeBar.value = playerCount.toFloat()

        activity.createThemedBuilder()
            .setTitle(R.string.menu_recommended_player_count)
            .setPositiveButton(R.string.set) { _, _ ->
                viewModel.addFilter(RecommendedPlayerCountFilterer(activity).apply {
                    this.playerCount = binding.rangeBar.value.toInt()
                    recommendation = if (binding.autocompleteView.text.toString() == bestString)
                        RecommendedPlayerCountFilterer.BEST
                    else
                        RecommendedPlayerCountFilterer.RECOMMENDED
                })
            }
            .setNegativeButton(R.string.clear) { _, _ ->
                viewModel.removeFilter(getType(activity))
            }
            .setView(binding.root)
            .create()
            .show()
    }

    override fun getType(context: Context) = RecommendedPlayerCountFilterer(context).type
}
