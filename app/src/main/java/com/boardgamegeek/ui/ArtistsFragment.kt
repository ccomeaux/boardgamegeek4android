package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentArtistsBinding
import com.boardgamegeek.databinding.RowArtistBinding
import com.boardgamegeek.model.Person
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnail
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.ArtistsViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class ArtistsFragment : Fragment() {
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<ArtistsViewModel>()
    private val adapter: ArtistsAdapter by lazy { ArtistsAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            RecyclerSectionItemDecoration(resources.getDimensionPixelSize(R.dimen.recycler_section_header_height), adapter)
        )

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.artists.observe(viewLifecycleOwner) {
            adapter.artists = it
            binding.recyclerView.isVisible = adapter.itemCount > 0
            binding.emptyTextView.isVisible = adapter.itemCount == 0
            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.progress.observe(viewLifecycleOwner) {
            if (it == null) {
                binding.horizontalProgressBar.progressContainer.isVisible = false
            } else {
                binding.horizontalProgressBar.progressContainer.isVisible = it.second > 0
                binding.horizontalProgressBar.progressView.max = it.second
                binding.horizontalProgressBar.progressView.progress = it.first
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ArtistsAdapter(private val viewModel: ArtistsViewModel) : RecyclerView.Adapter<ArtistsAdapter.ArtistViewHolder>(), AutoUpdatableAdapter,
        RecyclerSectionItemDecoration.SectionCallback {
        var artists: List<Person> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
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

        inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val binding = RowArtistBinding.bind(itemView)

            fun bind(artist: Person?) {
                artist?.let { a ->
                    binding.avatarView.loadThumbnail(a.thumbnailUrl, R.drawable.person_image_empty)
                    binding.nameView.text = a.name
                    binding.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, a.itemCount, a.itemCount)
                    binding.whitmoreScoreView.text = itemView.context.getString(R.string.whitmore_score).plus(" ${a.whitmoreScore}")
                    itemView.setOnClickListener {
                        PersonActivity.startForArtist(itemView.context, a.id, a.name)
                    }
                }
            }
        }
    }
}
