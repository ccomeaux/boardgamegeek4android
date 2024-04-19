package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.databinding.FragmentGameCollectionBinding
import com.boardgamegeek.model.Game
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.ui.adapter.GameCollectionItemAdapter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameCollectionFragment : Fragment() {
    private var _binding: FragmentGameCollectionBinding? = null
    private val binding get() = _binding!!
    private val adapter: GameCollectionItemAdapter by lazy { GameCollectionItemAdapter(requireContext()) }
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.isEnabled = false
        binding.swipeRefresh.setBggColors()

        binding.syncTimestamp.timestamp = 0L

        binding.recyclerView.setHasFixedSize(false)
        binding.recyclerView.adapter = adapter

        viewModel.game.observe(viewLifecycleOwner) {
            adapter.gameYearPublished = it?.yearPublished ?: Game.YEAR_UNKNOWN
        }

        viewModel.itemsAreRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it ?: false
        }
        viewModel.collectionItems.observe(viewLifecycleOwner) { items ->
            if (items == null){
                binding.emptyMessage.isVisible = true
            } else {
                adapter.items = items
                if (items.isNotEmpty()) {
                    binding.syncTimestamp.timestamp = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
                    binding.emptyMessage.isVisible = false
                } else {
                    binding.syncTimestamp.timestamp = System.currentTimeMillis()
                    binding.emptyMessage.isVisible = true
                }
                binding.swipeRefresh.setOnRefreshListener { viewModel.refreshItems() }
                binding.swipeRefresh.isEnabled = true
            }
            binding.contentLoadingProgressBar.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}
