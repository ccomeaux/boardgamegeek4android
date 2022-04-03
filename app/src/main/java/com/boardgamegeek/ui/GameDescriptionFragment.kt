package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameDescriptionBinding
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.ui.viewmodel.GameViewModel

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

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.footer.lastModifiedView.timestamp = 0L

        viewModel.gameId.observe(viewLifecycleOwner) {
            binding.footer.gameIdView.text = it.toString()
        }

        viewModel.game.observe(viewLifecycleOwner) {
            it?.let {
                binding.swipeRefresh.isRefreshing = it.status == Status.REFRESHING
                when {
                    it.status == Status.ERROR && it.data == null -> showError(it.message)
                    it.data == null -> showError(getString(R.string.empty_game))
                    else -> showData(it.data)
                }
                binding.progress.hide()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessage.text = message
            binding.emptyMessage.isVisible = true
            binding.gameDescription.isVisible = false
        }
    }

    private fun showData(game: GameEntity) {
        binding.emptyMessage.isVisible = false

        binding.gameDescription.setTextMaybeHtml(game.description)
        binding.gameDescription.isVisible = true

        binding.footer.gameIdView.text = game.id.toString()
        binding.footer.lastModifiedView.timestamp = game.updated
    }
}
