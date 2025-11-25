package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPersonDescriptionBinding
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.ui.viewmodel.PersonViewModel

class PersonDescriptionFragment : Fragment() {
    private var _binding: FragmentPersonDescriptionBinding? = null
    private val binding get() = _binding!!
    private var emptyMessageDescription = ""

    private val viewModel: PersonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(PersonViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPersonDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        emptyMessageDescription = getString(R.string.title_person).lowercase()
        binding.lastUpdated.timestamp = 0L

        viewModel.person.observe(this, Observer {
            binding.idView.text = it.id.toString()
            emptyMessageDescription = when (it.type) {
                PersonViewModel.PersonType.ARTIST -> getString(R.string.title_artist).lowercase()
                PersonViewModel.PersonType.DESIGNER -> getString(R.string.title_designer).lowercase()
                PersonViewModel.PersonType.PUBLISHER -> getString(R.string.title_publisher).lowercase()
            }
        })

        viewModel.details.observe(this, Observer {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_person, emptyMessageDescription))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_person, emptyMessageDescription))
                else -> showData(it.data)
            }
            binding.progress.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessageView.text = message
            binding.descriptionView.fadeOut()
            binding.emptyMessageView.fadeIn()
        }
    }

    private fun showData(person: PersonEntity) {
        if (person.description.isBlank()) {
            showError(getString(R.string.empty_person_description, emptyMessageDescription))
        } else {
            binding.descriptionView.setTextMaybeHtml(person.description)
            binding.descriptionView.fadeIn()
            binding.emptyMessageView.fadeOut()
        }
        binding.lastUpdated.timestamp = person.updatedTimestamp
    }

    companion object {
        @JvmStatic
        fun newInstance(): PersonDescriptionFragment {
            return PersonDescriptionFragment()
        }
    }
}
