package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentArtistsBinding
import com.boardgamegeek.databinding.RowArtistBinding
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.ArtistsViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import kotlin.properties.Delegates

class ArtistsFragment : Fragment(R.layout.fragment_artists) {
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArtistsViewModel by lazy {
        ViewModelProvider(this).get(ArtistsViewModel::class.java)
    }

    private val adapter: ArtistsAdapter by lazy {
        ArtistsAdapter(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentArtistsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter))

        viewModel.artists.observe(this, Observer {
            showData(it)
            binding.progressBar.hide()
        })

        viewModel.progress.observe(this, Observer {
            if (it == null) {
                binding.progressContainer.isVisible = false
            } else {
                binding.progressContainer.isVisible = it.second > 0
                binding.progressView.max = it.second
                binding.progressView.progress = it.first
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(artists: List<PersonEntity>) {
        adapter.artists = artists
        if (adapter.itemCount == 0) {
            binding.recyclerView.fadeOut()
            binding.emptyTextView.fadeIn()
        } else {
            binding.recyclerView.fadeIn()
            binding.emptyTextView.fadeOut()
        }
    }

    companion object {
        fun newInstance(): ArtistsFragment {
            return ArtistsFragment()
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
            val binding = RowArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ArtistViewHolder(binding)
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

        inner class ArtistViewHolder(private val binding: RowArtistBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(artist: PersonEntity?) {
                artist?.let { a ->
                    binding.avatarView.loadThumbnailInList(a.thumbnailUrl, R.drawable.person_image_empty)
                    binding.nameView.text = a.name
                    binding.countView.text = binding.root.context.resources.getQuantityString(R.plurals.games_suffix, a.itemCount, a.itemCount)
                    binding.whitmoreScoreView.text = binding.root.context.getString(R.string.whitmore_score).plus(" ${a.whitmoreScore}")
                    binding.root.setOnClickListener {
                        PersonActivity.startForArtist(binding.root.context, a.id, a.name)
                    }
                }
            }
        }
    }
}