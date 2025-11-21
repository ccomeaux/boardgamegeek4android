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
import com.boardgamegeek.databinding.FragmentPublishersBinding
import com.boardgamegeek.databinding.RowPublisherBinding
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.PublishersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import kotlin.properties.Delegates

class PublishersFragment : Fragment(R.layout.fragment_publishers) {
    private var _binding: FragmentPublishersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PublishersViewModel by lazy {
        ViewModelProvider(this).get(PublishersViewModel::class.java)
    }

    private val adapter: PublisherAdapter by lazy {
        PublisherAdapter(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPublishersBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter))

        viewModel.publishers.observe(viewLifecycleOwner, Observer {
            showData(it)
            binding.progressBar.hide()
        })

        viewModel.progress.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                binding.progress.progressContainer.isVisible = false
            } else {
                binding.progress.progressContainer.isVisible = it.second > 0
                binding.progress.progressView.max = it.second
                binding.progress.progressView.progress = it.first
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(publishers: List<CompanyEntity>) {
        adapter.publishers = publishers
        if (adapter.itemCount == 0) {
            binding.recyclerView.fadeOut()
            binding.emptyTextView.fadeIn()
        } else {
            binding.recyclerView.fadeIn()
            binding.emptyTextView.fadeOut()
        }
    }

    companion object {
        fun newInstance(): PublishersFragment {
            return PublishersFragment()
        }
    }

    class PublisherAdapter(private val viewModel: PublishersViewModel) : RecyclerView.Adapter<PublisherAdapter.PublisherViewHolder>(), AutoUpdatableAdapter, RecyclerSectionItemDecoration.SectionCallback {
        var publishers: List<CompanyEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = publishers.size

        override fun getItemId(position: Int) = publishers.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublisherViewHolder {
            val binding = RowPublisherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PublisherViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PublisherViewHolder, position: Int) {
            holder.bind(publishers.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (publishers.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = viewModel.getSectionHeader(publishers.getOrNull(position))
            val lastLetter = viewModel.getSectionHeader(publishers.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                publishers.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(publishers.getOrNull(position))
            }
        }

        inner class PublisherViewHolder(private val binding: RowPublisherBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(publisher: CompanyEntity?) {
                publisher?.let { p ->
                    binding.thumbnailView.loadThumbnailInList(p.thumbnailUrl)
                    binding.nameView.text = p.name
                    binding.countView.text = binding.root.context.resources.getQuantityString(R.plurals.games_suffix, p.itemCount, p.itemCount)
                    binding.whitmoreScoreView.text = binding.root.context.getString(R.string.whitmore_score).plus(" ${p.whitmoreScore}")
                    binding.root.setOnClickListener {
                        PersonActivity.startForPublisher(binding.root.context, p.id, p.name)
                    }
                }
            }
        }
    }
}