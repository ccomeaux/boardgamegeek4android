package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameDescriptionBinding
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.ui.viewmodel.GameViewModel

class GameDescriptionFragment : Fragment() {
    private var _binding: FragmentGameDescriptionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(this).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.lastModifiedView.timestamp = 0L

        viewModel.gameId.observe(this, Observer {
            binding.gameIdView.text = it.toString()
        })

        viewModel.game.observe(this, Observer {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> showData(it.data)
            }
            binding.progress.hide()
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessage.text = message
            binding.gameDescription.fadeOut()
            binding.emptyMessage.fadeIn()
        }
    }

    private fun showData(game: GameEntity) {
        binding.emptyMessage.fadeOut()

        binding.gameDescription.setTextMaybeHtml(game.description)
        binding.gameDescription.fadeIn()

        binding.gameIdView.text = game.id.toString()
        binding.lastModifiedView.timestamp = game.updated
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): GameDescriptionFragment {
            return GameDescriptionFragment()
        }
    }
}
