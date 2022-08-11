package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameCreditsBinding
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameDetailRow

class GameCreditsFragment : Fragment() {
    private var _binding: FragmentGameCreditsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameCreditsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dataContainer.layoutTransition.setAnimateParentHierarchy(false)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.footer.lastModifiedView.timestamp = 0

        viewModel.gameId.observe(viewLifecycleOwner) { gameId ->
            binding.footer.gameIdView.text = gameId.toString()
        }

        viewModel.game.observe(viewLifecycleOwner) {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> onGameContentChanged(it.data)
            }
            binding.progress.hide()

            viewModel.designers.observe(viewLifecycleOwner) { gameDetails -> onListQueryComplete(gameDetails, binding.designersInfoRow) }
            viewModel.artists.observe(viewLifecycleOwner) { gameDetails -> onListQueryComplete(gameDetails, binding.artistsInfoRow) }
            viewModel.publishers.observe(viewLifecycleOwner) { gameDetails -> onListQueryComplete(gameDetails, binding.publishersInfoRow) }
            viewModel.categories.observe(viewLifecycleOwner) { gameDetails -> onListQueryComplete(gameDetails, binding.categoriesInfoRow) }
            viewModel.mechanics.observe(viewLifecycleOwner) { gameDetails -> onListQueryComplete(gameDetails, binding.mechanicsInfoRow) }
        }
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessage.text = message
            binding.emptyMessage.isVisible = true
            binding.dataContainer.isVisible = false
        }
    }

    private fun onGameContentChanged(game: GameEntity) {
        listOf(
            binding.designersInfoRow,
            binding.artistsInfoRow,
            binding.publishersInfoRow,
            binding.categoriesInfoRow,
            binding.mechanicsInfoRow,
        ).forEach { it.colorize(game.iconColor) }

        binding.footer.gameIdView.text = game.id.toString()
        binding.footer.lastModifiedView.timestamp = game.updated

        binding.emptyMessage.isVisible = false
        binding.dataContainer.isVisible = true
    }

    private fun onListQueryComplete(list: List<GameDetailEntity>?, view: GameDetailRow) {
        view.bindData(
            viewModel.gameId.value ?: BggContract.INVALID_ID,
            viewModel.game.value?.data?.name.orEmpty(),
            list
        )
    }
}
