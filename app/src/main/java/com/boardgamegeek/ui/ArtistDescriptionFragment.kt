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
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.ArtistViewModel
import kotlinx.android.synthetic.main.fragment_artist_description.*

class ArtistDescriptionFragment : Fragment() {
    private var artistId: Int = 0

    private val viewModel: ArtistViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(ArtistViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_artist_description, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        artistId = arguments?.getInt(ARG_ARTIST_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        if (artistId == BggContract.INVALID_ID) throw IllegalArgumentException("Invalid artist ID")

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        artistIdView.text = artistId.toString()
        lastUpdated.timestamp = 0L

        viewModel.artist.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_artist, it.toString()))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_artist, it.toString()))
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
        emptyMessage.fadeOut()

        artistDescription.setTextMaybeHtml(artist.description)
        artistDescription.fadeIn()

        artistIdView.text = artist.id.toString()
        lastUpdated.timestamp = artist.updatedTimestamp
    }

    companion object {
        private const val ARG_ARTIST_ID = "ARTIST_ID"

        @JvmStatic
        fun newInstance(id: Int): ArtistDescriptionFragment {
            return ArtistDescriptionFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ARTIST_ID, id)
                }
            }
        }
    }
}
