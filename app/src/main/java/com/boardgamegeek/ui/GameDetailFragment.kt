package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.databinding.FragmentGameDetailsBinding
import com.boardgamegeek.ui.adapter.GameDetailAdapter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.viewmodel.GameViewModel.ProducerType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameDetailFragment : Fragment() {
    private var _binding: FragmentGameDetailsBinding? = null
    private val binding get() = _binding!!
    private val adapter: GameDetailAdapter by lazy { GameDetailAdapter() }

    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.producerType.observe(viewLifecycleOwner) {
            adapter.type = it ?: ProducerType.UNKNOWN
        }

        viewModel.producers.observe(viewLifecycleOwner) {
            adapter.items = it.orEmpty()
            binding.emptyMessage.isVisible = it.isNullOrEmpty()
            binding.recyclerView.isVisible = !it.isNullOrEmpty()
            binding.progressView.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
