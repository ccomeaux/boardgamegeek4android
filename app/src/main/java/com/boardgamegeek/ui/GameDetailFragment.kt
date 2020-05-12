package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GameDetailAdapter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.viewmodel.GameViewModel.ProducerType
import kotlinx.android.synthetic.main.fragment_game_details.*

class GameDetailFragment : Fragment() {
    private val adapter: GameDetailAdapter by lazy {
        GameDetailAdapter()
    }

    private val viewModel by activityViewModels<GameViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.producerType.observe(viewLifecycleOwner, Observer {
            adapter.type = it ?: ProducerType.UNKNOWN
        })

        viewModel.producers.observe(viewLifecycleOwner, Observer {
            if (it?.isNotEmpty() == true) {
                adapter.items = it
                emptyMessage?.fadeOut()
                recyclerView?.fadeIn()
            } else {
                adapter.items = emptyList()
                emptyMessage?.fadeIn()
                recyclerView?.fadeOut()
            }
            progressView?.hide()
        })
    }
}
