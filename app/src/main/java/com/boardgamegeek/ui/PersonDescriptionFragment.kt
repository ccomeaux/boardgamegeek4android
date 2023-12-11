package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPersonDescriptionBinding
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PersonDescriptionFragment : Fragment() {
    private var _binding: FragmentPersonDescriptionBinding? = null
    private val binding get() = _binding!!
    private var emptyMessageDescription = ""
    private val viewModel by activityViewModels<PersonViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPersonDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        emptyMessageDescription = getString(R.string.title_person).lowercase(Locale.getDefault())
        binding.lastUpdated.timestamp = 0L

        viewModel.id.observe(viewLifecycleOwner){
            binding.idView.text = it.toString()
        }
        viewModel.type.observe(viewLifecycleOwner) {
            it?.let {
                val resourceId = when (it) {
                    PersonViewModel.PersonType.ARTIST -> R.string.title_artist
                    PersonViewModel.PersonType.DESIGNER -> R.string.title_designer
                    PersonViewModel.PersonType.PUBLISHER -> R.string.title_publisher
                }
                emptyMessageDescription = getString(resourceId).lowercase(Locale.getDefault())
            }
        }

        viewModel.details.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING
            when {
                it == null -> showError(getString(R.string.empty_person, emptyMessageDescription))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_person, emptyMessageDescription))
                else -> showData(it.data)
            }
            binding.progress.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessageView.text = message
            binding.descriptionView.isVisible = false
            binding.emptyMessageView.isVisible = true
        }
    }

    private fun showData(person: Person) {
        if (person.description.isBlank()) {
            showError(getString(R.string.empty_person_description, emptyMessageDescription))
        } else {
            binding.descriptionView.setTextMaybeHtml(person.description)
            binding.descriptionView.fadeIn()
            binding.emptyMessageView.fadeOut()
        }
        binding.lastUpdated.timestamp = person.updatedTimestamp?.time ?: 0L
    }
}
