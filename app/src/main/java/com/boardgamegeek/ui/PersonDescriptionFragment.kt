package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import kotlinx.android.synthetic.main.fragment_person_description.*

class PersonDescriptionFragment : Fragment() {
    private var emptyMessageDescription = ""

    private val viewModel: PersonViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(PersonViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_person_description, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        emptyMessageDescription = getString(R.string.title_person).toLowerCase()
        lastUpdated.timestamp = 0L

        viewModel.person.observe(this, Observer {
            idView.text = it.second.toString()
            emptyMessageDescription = when (it.first) {
                PersonViewModel.PersonType.ARTIST -> {
                    getString(R.string.title_artist).toLowerCase()
                }
                PersonViewModel.PersonType.DESIGNER -> {
                    getString(R.string.title_designer).toLowerCase()
                }
            }
        })

        viewModel.details.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_person, emptyMessageDescription))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_person, emptyMessageDescription))
                else -> showData(it.data)
            }
            progress.hide()
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessageView?.text = message
            descriptionView.fadeOut()
            emptyMessageView.fadeIn()
        }
    }

    private fun showData(person: PersonEntity) {
        if (person.description.isBlank()) {
            showError(getString(R.string.empty_person_description, emptyMessageDescription))
        } else {
            descriptionView.text = person.description
            descriptionView.fadeIn()
            emptyMessageView.fadeOut()
        }
        lastUpdated.timestamp = person.updatedTimestamp
    }

    companion object {
        @JvmStatic
        fun newInstance(): PersonDescriptionFragment {
            return PersonDescriptionFragment()
        }
    }
}
