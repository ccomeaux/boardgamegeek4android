package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentTopGamesBinding
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.adapter.TopGamesAdapter
import com.boardgamegeek.ui.viewmodel.TopGamesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopGamesFragment : Fragment() {
    private var _binding: FragmentTopGamesBinding? = null
    private val binding get() = _binding!!
    private val adapter: TopGamesAdapter by lazy { TopGamesAdapter() }
    private val viewModel by activityViewModels<TopGamesViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTopGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.topGames.observe(viewLifecycleOwner) {
            it?.let {
                when (it.status) {
                    Status.ERROR -> {
                        displayEmpty(getString(R.string.empty_http_error, it.message))
                        binding.progressView.hide()
                    }
                    Status.SUCCESS -> {
                        if (it.data.isNullOrEmpty()) {
                            displayEmpty(getString(R.string.empty_top_games))
                        } else {
                            adapter.results = it.data
                            binding.recyclerView.isVisible = true
                            binding.emptyView.isVisible = false
                        }
                        binding.progressView.hide()
                    }
                    else -> {
                        binding.progressView.show()
                    }
                }
            }
        }
    }

    private fun displayEmpty(message: String) {
        binding.emptyView.text = message
        binding.recyclerView.isVisible = false
        binding.emptyView.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
