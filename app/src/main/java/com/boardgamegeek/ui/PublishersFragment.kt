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
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.PublishersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import kotlinx.android.synthetic.main.fragment_publishers.*
import kotlinx.android.synthetic.main.include_horizontal_progress.*
import kotlinx.android.synthetic.main.row_publisher.view.*
import kotlin.properties.Delegates

class PublishersFragment : Fragment(R.layout.fragment_publishers) {
    private val viewModel by activityViewModels<PublishersViewModel>()

    private val adapter: PublisherAdapter by lazy {
        PublisherAdapter(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter))

        viewModel.publishers.observe(viewLifecycleOwner, Observer {
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

    private fun showData(publishers: List<CompanyEntity>) {
        adapter.publishers = publishers
        if (adapter.itemCount == 0) {
            recyclerView.fadeOut()
            emptyTextView.fadeIn()
        } else {
            recyclerView.fadeIn()
            emptyTextView.fadeOut()
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
            return PublisherViewHolder(parent.inflate(R.layout.row_publisher))
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

        inner class PublisherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(publisher: CompanyEntity?) {
                publisher?.let { p ->
                    itemView.thumbnailView.loadThumbnailInList(p.thumbnailUrl)
                    itemView.nameView.text = p.name
                    itemView.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, p.itemCount, p.itemCount)
                    itemView.whitmoreScoreView.text = itemView.context.getString(R.string.whitmore_score).plus(" ${p.whitmoreScore}")
                    itemView.setOnClickListener {
                        PersonActivity.startForPublisher(itemView.context, p.id, p.name)
                    }
                }
            }
        }
    }
}