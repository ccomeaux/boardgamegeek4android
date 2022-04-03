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
import com.boardgamegeek.databinding.FragmentDesignersBinding
import com.boardgamegeek.databinding.RowDesignerBinding
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.DesignersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import kotlin.properties.Delegates

class DesignersFragment : Fragment() {
    private var _binding: FragmentDesignersBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<DesignersViewModel>()
    private val adapter: DesignersAdapter by lazy { DesignersAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDesignersBinding.inflate(inflater, container, false)
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

        viewModel.designers.observe(viewLifecycleOwner) {
            adapter.designers = it
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

    class DesignersAdapter(private val viewModel: DesignersViewModel) : RecyclerView.Adapter<DesignersAdapter.DesignerViewHolder>(),
        AutoUpdatableAdapter, RecyclerSectionItemDecoration.SectionCallback {
        var designers: List<PersonEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = designers.size

        override fun getItemId(position: Int) = designers.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DesignerViewHolder {
            return DesignerViewHolder(parent.inflate(R.layout.row_designer))
        }

        override fun onBindViewHolder(holder: DesignerViewHolder, position: Int) {
            holder.bind(designers.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (designers.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = viewModel.getSectionHeader(designers.getOrNull(position))
            val lastLetter = viewModel.getSectionHeader(designers.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                designers.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(designers.getOrNull(position))
            }
        }

        inner class DesignerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowDesignerBinding.bind(itemView)

            fun bind(designer: PersonEntity?) {
                designer?.let { d ->
                    binding.avatarView.loadThumbnailInList(d.thumbnailUrl, R.drawable.person_image_empty)
                    binding.nameView.text = d.name
                    binding.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, d.itemCount, d.itemCount)
                    binding.whitmoreScoreView.text = itemView.context.getString(R.string.whitmore_score).plus(" ${d.whitmoreScore}")
                    itemView.setOnClickListener {
                        PersonActivity.startForDesigner(itemView.context, d.id, d.name)
                    }
                }
            }
        }
    }
}
