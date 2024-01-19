package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentArtistsBinding
import com.boardgamegeek.databinding.RowArtistBinding
import com.boardgamegeek.model.Person
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnail
import com.boardgamegeek.ui.viewmodel.ArtistsViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import dagger.hilt.android.AndroidEntryPoint

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

        binding.swipeRefresh.setOnRefreshListener { viewModel.reload() }

        viewModel.artists.observe(viewLifecycleOwner) {
            adapter.submitList(it)
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

    class ArtistsAdapter(private val viewModel: ArtistsViewModel) :
        ListAdapter<Person, ArtistsAdapter.ArtistViewHolder>(ItemCallback()),
        RecyclerSectionItemDecoration.SectionCallback {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long = getItem(position).id.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
            return ArtistViewHolder(parent.inflate(R.layout.row_artist))
        }

        override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (currentList.isEmpty()) return false
            if (position == 0) return true
            val thisHeader = viewModel.getSectionHeader(currentList.getOrNull(position))
            val lastHeader = viewModel.getSectionHeader(currentList.getOrNull(position - 1))
            return thisHeader != lastHeader
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                currentList.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(currentList.getOrNull(position))
            }
        }

        class ItemCallback : DiffUtil.ItemCallback<Person>() {
            override fun areItemsTheSame(oldItem: Person, newItem: Person) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Person, newItem: Person) = oldItem == newItem
        }

        inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val binding = RowArtistBinding.bind(itemView)

            fun bind(artist: Person) {
                binding.avatarView.loadThumbnail(artist.thumbnailUrl, R.drawable.person_image_empty)
                binding.nameView.text = artist.name
                binding.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, artist.itemCount, artist.itemCount)
                binding.whitmoreScoreView.text = itemView.context.getString(R.string.whitmore_score).plus(" ${artist.whitmoreScore}")
                itemView.setOnClickListener {
                    PersonActivity.startForArtist(itemView.context, artist.id, artist.name)
                }
            }
        }
    }
}
