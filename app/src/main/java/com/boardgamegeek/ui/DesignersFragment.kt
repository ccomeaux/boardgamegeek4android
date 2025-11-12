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
import com.boardgamegeek.databinding.FragmentDesignersBinding
import com.boardgamegeek.databinding.RowDesignerBinding
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.DesignsViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import kotlin.properties.Delegates

class DesignersFragment : Fragment(R.layout.fragment_designers) {
    private var _binding: FragmentDesignersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DesignsViewModel by lazy {
        ViewModelProvider(this).get(DesignsViewModel::class.java)
    }

    private val adapter: DesignersAdapter by lazy {
        DesignersAdapter(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDesignersBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter))

        viewModel.designers.observe(this, Observer {
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

    private fun showData(designers: List<PersonEntity>) {
        adapter.designers = designers
        if (adapter.itemCount == 0) {
            binding.recyclerView.fadeOut()
            binding.emptyTextView.fadeIn()
        } else {
            binding.recyclerView.fadeIn()
            binding.emptyTextView.fadeOut()
        }
    }

    companion object {
        fun newInstance(): DesignersFragment {
            return DesignersFragment()
        }
    }

    class DesignersAdapter(private val viewModel: DesignsViewModel) : RecyclerView.Adapter<DesignersAdapter.DesignerViewHolder>(), AutoUpdatableAdapter, RecyclerSectionItemDecoration.SectionCallback {
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
            val binding = RowDesignerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DesignerViewHolder(binding)
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

        inner class DesignerViewHolder(private val binding: RowDesignerBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(designer: PersonEntity?) {
                designer?.let { d ->
                    binding.avatarView.loadThumbnailInList(d.thumbnailUrl, R.drawable.person_image_empty)
                    binding.nameView.text = d.name
                    binding.countView.text = binding.root.context.resources.getQuantityString(R.plurals.games_suffix, d.itemCount, d.itemCount)
                    binding.whitmoreScoreView.text = binding.root.context.getString(R.string.whitmore_score).plus(" ${d.whitmoreScore}")
                    binding.root.setOnClickListener {
                        PersonActivity.startForDesigner(binding.root.context, d.id, d.name)
                    }
                }
            }
        }
    }
}