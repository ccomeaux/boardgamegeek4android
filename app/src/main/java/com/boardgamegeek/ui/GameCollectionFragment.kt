package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameCollectionBinding
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.extensions.toast
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
            adapter.gameYearPublished = it?.data?.yearPublished ?: GameEntity.YEAR_UNKNOWN
        }

        viewModel.collectionItems.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                binding.swipeRefresh.isRefreshing = status == Status.REFRESHING
                if (status == Status.ERROR) {
                    showError(
                        message.ifBlank { getString(R.string.empty_game_collection) },
                        hasData = data?.isNotEmpty() ?: false,
                    )
                }
                showData(data.orEmpty())
                binding.progressView.hide()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(items: List<CollectionItemEntity>) {
        adapter.items = items
        if (items.isNotEmpty()) {
            binding.syncTimestamp.timestamp = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
            binding.emptyMessage.isVisible = false
        } else {
            binding.syncTimestamp.timestamp = System.currentTimeMillis()
            showError(hasData = false)
        }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.isEnabled = true
    }

    private fun showError(message: String = getString(R.string.empty_game_collection), hasData: Boolean = false) {
        if (hasData) {
            toast(message)
            binding.emptyMessage.isVisible = false
        } else {
            binding.emptyMessage.setTextOrHide(message)
        }
    }
}
