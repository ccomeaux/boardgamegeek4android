package com.boardgamegeek.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import kotlinx.android.synthetic.main.fragment_person_description.*
import java.util.*

class PersonDescriptionFragment : Fragment(R.layout.fragment_person_description) {
    private var emptyMessageDescription = ""

    private val viewModel by activityViewModels<PersonViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        emptyMessageDescription = getString(R.string.title_person).toLowerCase(Locale.getDefault())
        lastUpdated.timestamp = 0L

        viewModel.person.observe(viewLifecycleOwner, Observer {
            idView.text = it.id.toString()
            val resourceId = when (it.type) {
                PersonViewModel.PersonType.ARTIST -> R.string.title_artist
                PersonViewModel.PersonType.DESIGNER -> R.string.title_designer
                PersonViewModel.PersonType.PUBLISHER -> R.string.title_publisher
            }
            emptyMessageDescription = getString(resourceId).toLowerCase(Locale.getDefault())
        })

        viewModel.details.observe(viewLifecycleOwner, Observer {
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
            descriptionView.setTextMaybeHtml(person.description)
            descriptionView.fadeIn()
            emptyMessageView.fadeOut()
        }
        lastUpdated.timestamp = person.updatedTimestamp
    }
}
