package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.databinding.FragmentGameCollectionBinding
import com.boardgamegeek.ui.adapter.GameCollectionItemAdapter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import org.jetbrains.anko.support.v4.toast

class GameCollectionFragment : Fragment() {
    private var _binding: FragmentGameCollectionBinding? = null
    private val binding get() = _binding!!
    private val adapter: GameCollectionItemAdapter by lazy {
        GameCollectionItemAdapter()
    }

    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(requireActivity()).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        viewModel.game.observe(viewLifecycleOwner, Observer {
            adapter.gameYearPublished = it?.data?.yearPublished ?: YEAR_UNKNOWN
        })

        viewModel.collectionItems.observe(viewLifecycleOwner, Observer {
            if (_binding == null) return@Observer
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError()
                it.status == Status.ERROR -> {
                    val errorMessage = if (it.message.isNotBlank()) it.message else getString(R.string.empty_game_collection)
                    if (it.data?.isNotEmpty() == true) {
                        showData(it.data)
                        showError(errorMessage, true)
                    } else {
                        showError(errorMessage, false)
                    }
                }
                else -> showData(it.data ?: emptyList())
            }
            binding.progressView.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(items: List<CollectionItemEntity>) {
        if (!isAdded) return
        if (items.isNotEmpty()) {
            adapter.items = items
            binding.syncTimestamp.timestamp = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
            binding.emptyMessage.fadeOut()
            binding.recyclerView.fadeIn()
        } else {
            binding.syncTimestamp.timestamp = System.currentTimeMillis()
            showError()
            binding.recyclerView.fadeOut()
        }
        binding.swipeRefresh.setOnRefreshListener {
            if (items.any { it.isDirty })
                SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
            viewModel.refresh()
        }
        binding.swipeRefresh.isEnabled = true
    }

    private fun showError(message: String = getString(R.string.empty_game_collection), hasData: Boolean = false) {
        if (hasData) {
            toast(message)
        } else {
            binding.emptyMessage.text = message
            binding.emptyMessage.fadeIn()
        }
    }

    companion object {
        fun newInstance(): GameCollectionFragment {
            return GameCollectionFragment()
        }
    }
}
