package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.entities.ArtistEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.ui.viewmodel.ArtistViewModel
import kotlinx.android.synthetic.main.fragment_artist_description.*

class ArtistDescriptionFragment : Fragment() {
    private val viewModel: ArtistViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(ArtistViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_artist_description, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        lastUpdated.timestamp = 0L

        viewModel.artistId.observe(this, Observer {
            artistIdView.text = it.toString()
        })

        viewModel.artist.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_artist))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_artist))
                else -> showData(it.data)
            }
            progress.hide()
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessage?.text = message
            artistDescription.fadeOut()
            emptyMessage.fadeIn()
        }
    }

    private fun showData(artist: ArtistEntity) {
        if (artist.description.isBlank()) {
            showError(getString(R.string.empty_artist_description))
        } else {
            artistDescription.text = artist.description
            artistDescription.fadeIn()
            emptyMessage.fadeOut()
        }
        lastUpdated.timestamp = artist.updatedTimestamp
    }

    companion object {
        @JvmStatic
        fun newInstance(): ArtistDescriptionFragment {
            return ArtistDescriptionFragment()
        }
    }
}
