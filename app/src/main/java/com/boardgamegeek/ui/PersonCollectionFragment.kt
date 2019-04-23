package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.PersonCollectionAdapter
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import kotlinx.android.synthetic.main.fragment_game_details.*

class PersonCollectionFragment : Fragment() {
    private var emptyMessageDescription = ""

    private val adapter: PersonCollectionAdapter by lazy {
        PersonCollectionAdapter()
    }

    private val viewModel: PersonViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(PersonViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_person_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        emptyMessageDescription = getString(R.string.title_person).toLowerCase()
        viewModel.person.observe(this, Observer {
            emptyMessageDescription = when (it.first) {
                PersonViewModel.PersonType.ARTIST -> getString(R.string.title_artist).toLowerCase()
                PersonViewModel.PersonType.DESIGNER -> getString(R.string.title_designer).toLowerCase()
            }
        })
        viewModel.collection.observe(this, Observer {
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

    companion object {
        @JvmStatic
        fun newInstance(): PersonCollectionFragment {
            return PersonCollectionFragment()
        }
    }
}