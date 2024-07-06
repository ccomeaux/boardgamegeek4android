package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.databinding.FragmentGameDescriptionBinding
import com.boardgamegeek.model.Game
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameDescriptionFragment : Fragment() {
    private var _binding: FragmentGameDescriptionBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshGame() }
        binding.swipeRefresh.setBggColors()

        binding.footer.lastModifiedView.timestamp = 0L

        viewModel.gameId.observe(viewLifecycleOwner) {
            binding.footer.gameIdView.text = it.toString()
        }

        viewModel.gameIsRefreshing.observe(viewLifecycleOwner) {
            it?.let { binding.swipeRefresh.isRefreshing }
        }
        viewModel.game.observe(viewLifecycleOwner) {
            if (it == null) {
                binding.emptyMessage.isVisible = true
                binding.gameDescription.isVisible = false
                binding.footer.gameIdView.isVisible = false
                binding.footer.lastModifiedView.isVisible = false
            } else {
                showData(it)
            }
            binding.contentLoadingProgressBar.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(game: Game) {
        binding.emptyMessage.isVisible = false

        binding.gameDescription.setTextMaybeHtml(game.description)
        binding.gameDescription.isVisible = true

        binding.footer.gameIdView.text = game.id.toString()
        binding.footer.lastModifiedView.timestamp = game.updated
        binding.footer.gameIdView.isVisible = true
        binding.footer.lastModifiedView.isVisible = true
    }
}
