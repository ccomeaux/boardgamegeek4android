package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameDetailsBinding
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GameDetailAdapter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.viewmodel.GameViewModel.ProducerType

class GameDetailFragment : Fragment() {
    private var _binding: FragmentGameDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val adapter: GameDetailAdapter by lazy {
        GameDetailAdapter()
    }

    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(this).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.producerType.observe(this, Observer {
            adapter.type = it ?: ProducerType.UNKNOWN
        })

        viewModel.producers.observe(this, Observer {
            if (it?.isNotEmpty() == true) {
                adapter.items = it
                binding.emptyMessage.fadeOut()
                binding.recyclerView.fadeIn()
            } else {
                adapter.items = emptyList()
                binding.emptyMessage.fadeIn()
                binding.recyclerView.fadeOut()
            }
            binding.progressView.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
