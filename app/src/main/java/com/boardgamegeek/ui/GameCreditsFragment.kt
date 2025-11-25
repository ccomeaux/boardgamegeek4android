package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameCreditsBinding
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameDetailRow

class GameCreditsFragment : Fragment() {
    private var _binding: FragmentGameCreditsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(requireActivity()).get(GameViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameCreditsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.footer.lastModifiedView.timestamp = 0

        viewModel.gameId.observe(viewLifecycleOwner, Observer { gameId ->
            binding.footer.gameIdView.text = gameId.toString()
        })

        viewModel.game.observe(viewLifecycleOwner, Observer {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> onGameContentChanged(it.data)
            }
            binding.progress.hide()

            viewModel.designers.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, binding.gameInfoDesigners) })

            viewModel.artists.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, binding.gameInfoArtists) })

            viewModel.publishers.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, binding.gameInfoPublishers) })

            viewModel.categories.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, binding.gameInfoCategories) })

            viewModel.mechanics.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, binding.gameInfoMechanics) })
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessage.text = message
            binding.dataContainer.fadeOut()
            binding.emptyMessage.fadeIn()
        }
    }

    private fun colorize(@ColorInt iconColor: Int) {
        if (!isAdded) return

        listOf(binding.gameInfoDesigners, binding.gameInfoArtists, binding.gameInfoPublishers, binding.gameInfoCategories, binding.gameInfoMechanics)
                .forEach { it.colorize(iconColor) }
    }

    private fun onGameContentChanged(game: GameEntity) {
        colorize(game.iconColor)

        binding.footer.gameIdView.text = game.id.toString()
        binding.footer.lastModifiedView.timestamp = game.updated

        binding.emptyMessage.fadeOut()
        binding.dataContainer.fadeIn()
    }

    private fun onListQueryComplete(list: List<GameDetailEntity>?, view: GameDetailRow?) {
        view?.bindData(
                viewModel.gameId.value ?: BggContract.INVALID_ID,
                viewModel.game.value?.data?.name ?: "",
                list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): GameCreditsFragment {
            return GameCreditsFragment()
        }
    }
}