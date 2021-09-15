package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.ArtistsViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.android.synthetic.main.include_horizontal_progress.*
import kotlinx.android.synthetic.main.row_artist.view.*
import kotlin.properties.Delegates

class ArtistsFragment : Fragment(R.layout.fragment_artists) {
    private val viewModel by activityViewModels<ArtistsViewModel>()

    private val adapter: ArtistsAdapter by lazy {
        ArtistsAdapter(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter))

        viewModel.artists.observe(viewLifecycleOwner, Observer {
            showData(it)
            progressBar.hide()
        })

        viewModel.progress.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                progressContainer.isVisible = false
            } else {
                progressContainer.isVisible = it.second > 0
                progressView.max = it.second
                progressView.progress = it.first
            }
        })
    }

    private fun showData(artists: List<PersonEntity>) {
        adapter.artists = artists
        if (adapter.itemCount == 0) {
            recyclerView.fadeOut()
            emptyTextView.fadeIn()
        } else {
            recyclerView.fadeIn()
            emptyTextView.fadeOut()
        }
    }

    class ArtistsAdapter(private val viewModel: ArtistsViewModel) : RecyclerView.Adapter<ArtistsAdapter.ArtistViewHolder>(), AutoUpdatableAdapter, RecyclerSectionItemDecoration.SectionCallback {
        var artists: List<PersonEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = artists.size

        override fun getItemId(position: Int) = artists.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
            return ArtistViewHolder(parent.inflate(R.layout.row_artist))
        }

        override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
            holder.bind(artists.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (artists.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = viewModel.getSectionHeader(artists.getOrNull(position))
            val lastLetter = viewModel.getSectionHeader(artists.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                artists.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(artists.getOrNull(position))
            }
        }

        inner class ArtistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(artist: PersonEntity?) {
                artist?.let { a ->
                    itemView.avatarView.loadThumbnailInList(a.thumbnailUrl, R.drawable.person_image_empty)
                    itemView.nameView.text = a.name
                    itemView.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, a.itemCount, a.itemCount)
                    itemView.whitmoreScoreView.text = itemView.context.getString(R.string.whitmore_score).plus(" ${a.whitmoreScore}")
                    itemView.setOnClickListener {
                        PersonActivity.startForArtist(itemView.context, a.id, a.name)
                    }
                }
            }
        }
    }
}