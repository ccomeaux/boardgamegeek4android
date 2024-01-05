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
import com.boardgamegeek.databinding.FragmentPublishersBinding
import com.boardgamegeek.databinding.RowPublisherBinding
import com.boardgamegeek.model.Company
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnail
import com.boardgamegeek.ui.viewmodel.PublishersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration

class PublishersFragment : Fragment() {
    private var _binding: FragmentPublishersBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<PublishersViewModel>()
    private val adapter: PublisherAdapter by lazy { PublisherAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPublishersBinding.inflate(inflater, container, false)
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

        viewModel.publishers.observe(viewLifecycleOwner) {
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

    class PublisherAdapter(private val viewModel: PublishersViewModel) :
        ListAdapter<Company, PublisherAdapter.PublisherViewHolder>(ItemCallback()),
        RecyclerSectionItemDecoration.SectionCallback {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int) = getItem(position).id.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublisherViewHolder {
            return PublisherViewHolder(parent.inflate(R.layout.row_publisher))
        }

        override fun onBindViewHolder(holder: PublisherViewHolder, position: Int) {
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

        class ItemCallback : DiffUtil.ItemCallback<Company>() {
            override fun areItemsTheSame(oldItem: Company, newItem: Company) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Company, newItem: Company) = oldItem == newItem
        }

        inner class PublisherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val binding = RowPublisherBinding.bind(itemView)

            fun bind(publisher: Company) {
                binding.thumbnailView.loadThumbnail(publisher.thumbnailUrl)
                binding.nameView.text = publisher.name
                binding.countView.text =
                    itemView.context.resources.getQuantityString(R.plurals.games_suffix, publisher.itemCount, publisher.itemCount)
                binding.whitmoreScoreView.text = itemView.context.getString(R.string.whitmore_score).plus(" ${publisher.whitmoreScore}")
                itemView.setOnClickListener {
                    PersonActivity.startForPublisher(itemView.context, publisher.id, publisher.name)
                }
            }
        }
    }
}
